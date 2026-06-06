const prisma = require('../prismaClient');
const whoisService = require('../services/analysis/whois.service');
const ipgeoService = require('../services/analysis/ipgeo.service');
const sslService = require('../services/analysis/ssl.service');
const virustotalService = require('../services/analysis/virustotal.service');
const domainCheckService = require('../services/analysis/domainCheck.service');
const puppeteerService = require('../services/sandbox/puppeteer.service');
const openaiService = require('../services/ai/openai.service');

exports.runAnalysis = async (req, res, next) => {
  try {
    const { caseId } = req.body;
    if (!caseId) return res.status(400).json({ error: 'Missing caseId' });

    const c = await prisma.case.findUnique({ where: { id: caseId } });
    if (!c) return res.status(404).json({ error: 'Case not found' });

    const url = c.url || 'http://example.com';

    // Run parallel checks
    const [
      whoisResult,
      ipgeoResult,
      sslResult,
      virustotalResult,
      similarityResult,
      sandboxResult
    ] = await Promise.allSettled([
      whoisService.getWhoisData(url),
      ipgeoService.getIpGeoData(url),
      sslService.getSslInfo(url),
      virustotalService.getVirusTotalResult(url),
      domainCheckService.checkSimilarity(url),
      puppeteerService.runSandbox(url)
    ]);

    const whois = whoisResult.status === 'fulfilled' && !whoisResult.value?.error ? whoisResult.value : null;
    const ipGeo = ipgeoResult.status === 'fulfilled' && ipgeoResult.value !== null ? ipgeoResult.value : null;
    const ssl = sslResult.status === 'fulfilled' ? sslResult.value : { valid: false, error: 'SSL check failed' };
    const virustotal = virustotalResult.status === 'fulfilled' ? virustotalResult.value : null;
    const similarity = similarityResult.status === 'fulfilled' ? similarityResult.value : null;
    const sandbox = sandboxResult.status === 'fulfilled' ? sandboxResult.value : {};

    // Format results to run GPT analysis
    const analysisData = {
      url,
      whois,
      ipGeo,
      ssl,
      virustotal,
      similarity,
      redirectChain: sandbox.redirectChain || [url]
    };

    // AI Analysis
    const aiResult = await openaiService.analyzeWithAi(analysisData);

    const severityMap = {
      low: 'Low',
      medium: 'Medium',
      high: 'High',
      critical: 'Critical'
    };
    const rawSeverity = String(aiResult.severity).toLowerCase();
    const severity = severityMap[rawSeverity] || 'Low';

    // Save to Database
    const analysis = await prisma.analysis.create({
      data: {
        caseId: c.id,
        threat_score: aiResult.threat_score,
        severity: severity,
        whois_data: whois,
        ip_geolocation: ipGeo,
        ssl_info: ssl,
        redirect_chain: sandbox.redirectChain || [url],
        virustotal_result: virustotal,
        page_screenshot: sandbox.screenshot || null,
        page_source_hash: sandbox.pageSourceHash || null,
        ai_summary: aiResult.ai_summary,
        ai_indicators: aiResult.indicators,
        ai_techniques: aiResult.techniques,
        sandbox_version: 'Puppeteer Headless 1.0'
      }
    });

    // Update case status to Investigating
    await prisma.case.update({
      where: { id: c.id },
      data: { status: 'Investigating' }
    });

    // Audit Log Case Analyzed
    await prisma.auditLog.create({
      data: {
        userId: req.user?.userId || c.userId,
        caseId: c.id,
        action: 'CASE_ANALYZED',
        ip_address: req.ip || null,
        metadata: { threatScore: aiResult.threat_score, severity: severity }
      }
    });

    res.status(201).json(analysis);
  } catch (err) {
    console.error('Analysis Engine Error:', err);
    next(err);
  }
};

exports.getAnalysisByCase = async (req, res, next) => {
  try {
    const { caseId } = req.params;
    const analysis = await prisma.analysis.findFirst({
      where: { caseId },
      orderBy: { analyzed_at: 'desc' }
    });
    if (!analysis) return res.status(404).json({ error: 'No analysis found for this case' });
    res.json(analysis);
  } catch (err) {
    next(err);
  }
};

exports.getScreenshot = async (req, res, next) => {
  try {
    const { caseId } = req.params;
    const analysis = await prisma.analysis.findFirst({
      where: { caseId },
      orderBy: { analyzed_at: 'desc' }
    });
    if (!analysis || !analysis.page_screenshot) {
      return res.status(404).json({ error: 'Screenshot not found' });
    }
    
    const screenshot = analysis.page_screenshot;
    if (screenshot.startsWith('data:image/png;base64,')) {
      const img = Buffer.from(screenshot.replace('data:image/png;base64,', ''), 'base64');
      res.writeHead(200, {
        'Content-Type': 'image/png',
        'Content-Length': img.length
      });
      res.end(img);
    } else {
      res.json({ screenshot });
    }
  } catch (err) {
    next(err);
  }
};
