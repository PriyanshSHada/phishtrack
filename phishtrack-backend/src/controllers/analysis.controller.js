const prisma = require('../prismaClient');
const whoisService = require('../services/analysis/whois.service');
const ipgeoService = require('../services/analysis/ipgeo.service');
const sslService = require('../services/analysis/ssl.service');
const virustotalService = require('../services/analysis/virustotal.service');
const domainCheckService = require('../services/analysis/domainCheck.service');
const homographService = require('../services/analysis/homograph.service');
const puppeteerService = require('../services/sandbox/puppeteer.service');
const openaiService = require('../services/ai/openai.service');
const logger = require('../utils/logger');

exports.runAnalysis = async (req, res, next) => {
  try {
    const { caseId } = req.body;
    if (!caseId) return res.status(400).json({ error: 'Missing caseId' });

    const c = await prisma.case.findUnique({ where: { id: caseId } });
    if (!c) return res.status(404).json({ error: 'Case not found' });
    if (c.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    const isIpCase = c.target_type === 'IP';
    const targetIp = c.target_ip;
    const url = c.url || (targetIp ? `http://${targetIp}` : 'http://example.com');

    let whois = null;
    let ipGeo = null;
    let ssl = { valid: false, error: 'SSL check skipped for IP-only case' };
    let virustotal = null;
    let similarity = null;
    let homograph = null;
    let sandbox = {};

    if (isIpCase && targetIp) {
      if (sandboxResult.status === 'fulfilled' && sandboxResult.value?.status === 'DEAD_LINK') {
        logger.warn(`Skipping IP Geo and VirusTotal for DEAD_LINK: ${url}`, { service: 'phishtrack-api' });
        ipGeo = null;
        virustotal = null;
        sandbox = sandboxResult.value;
      } else {
        const [ipgeoResult, virustotalResult] = await Promise.allSettled([
          ipgeoService.getIpGeoDataFromIp(targetIp),
          virustotalService.getVirusTotalIpResult(targetIp)
        ]);
        ipGeo = ipgeoResult.status === 'fulfilled' && ipgeoResult.value !== null ? ipgeoResult.value : null;
        virustotal = virustotalResult.status === 'fulfilled' ? virustotalResult.value : null;
        sandbox = sandboxResult.status === 'fulfilled' ? sandboxResult.value : {};
      }

      if (ipGeo) {
        whois = {
          raw: "IP WHOIS data extracted from Regional Internet Registry (RIR) / GeoIP.",
          domain: targetIp,
          registrar: ipGeo.org || ipGeo.isp || ipGeo.as || 'Unknown',
          country: ipGeo.country || ipGeo.countryCode || 'Unknown',
          creationDate: null,
          expiryDate: null,
          ageDays: null,
          isSuspiciousAge: false
        };
      }
    } else {
      const sandboxResult = await puppeteerService.runSandbox(url);
      sandbox = sandboxResult;

      if (sandbox.status === 'DEAD_LINK') {
        logger.warn(`Skipping WHOIS, IP Geo, SSL, VirusTotal, Similarity, Homograph for DEAD_LINK: ${url}`, { service: 'phishtrack-api' });
      } else {
        const [
          whoisResult,
          ipgeoResult,
          sslResult,
          virustotalResult,
          similarityResult,
          homographResult
        ] = await Promise.allSettled([
          whoisService.getWhoisData(url),
          ipgeoService.getIpGeoData(url),
          sslService.getSslInfo(url),
          virustotalService.getVirusTotalResult(url),
          domainCheckService.checkSimilarity(url),
          homographService.detectHomographAttack(url)
        ]);

        whois = whoisResult.status === 'fulfilled' && !whoisResult.value?.error ? whoisResult.value : null;
        ipGeo = ipgeoResult.status === 'fulfilled' && ipgeoResult.value !== null ? ipgeoResult.value : null;
        ssl = sslResult.status === 'fulfilled' ? sslResult.value : { valid: false, error: 'SSL check failed' };
        virustotal = virustotalResult.status === 'fulfilled' ? virustotalResult.value : null;
        similarity = similarityResult.status === 'fulfilled' ? similarityResult.value : null;
        homograph = homographResult.status === 'fulfilled' ? homographResult.value : null;
      }
    }

    const analysisData = {
      url: isIpCase ? targetIp : url,
      targetType: c.target_type,
      whois,
      ipGeo,
      ssl,
      virustotal,
      similarity,
      homograph,
      redirectChain: sandbox.redirectChain || [isIpCase ? targetIp : url]
    };

    const aiResult = await openaiService.analyzeWithAi(analysisData);

    // Safety Net: Override AI if VirusTotal is heavily flagging the site
    const vtMalicious = virustotal?.maliciousCount || 0;
    if (vtMalicious >= 5 && aiResult.threat_score < 70) {
      aiResult.threat_score = Math.max(aiResult.threat_score, 85);
      aiResult.verdict = 'Malware Distribution';
      aiResult.severity = 'High';
      aiResult.ai_summary = `[OVERRIDE] VirusTotal detected this target as malicious on ${vtMalicious} engines. ` + (aiResult.ai_summary || '');
      aiResult.confidence = 99;
    }

    const severityMap = {
      low: 'Low',
      medium: 'Medium',
      high: 'High',
      critical: 'Critical'
    };
    const rawSeverity = String(aiResult.severity).toLowerCase();
    const severity = severityMap[rawSeverity] || 'Low';

    const analysis = await prisma.analysis.create({
      data: {
        caseId: c.id,
        threat_score: aiResult.threat_score,
        confidence: aiResult.confidence || 50,
        severity: severity,
        verdict: aiResult.verdict || 'Suspicious',
        brand_impersonated: aiResult.brand_impersonated || null,
        whois_data: whois,
        ip_geolocation: ipGeo,
        ssl_info: ssl,
        redirect_chain: sandbox.redirectChain || [isIpCase ? targetIp : url],
        virustotal_result: virustotal,
        page_screenshot: sandbox.screenshot || null,
        page_source_hash: sandbox.pageSourceHash || null,
        ai_summary: aiResult.ai_summary,
        ai_indicators: aiResult.indicators,
        ai_techniques: aiResult.techniques,
        mitre_techniques: aiResult.mitre_techniques || [],
        sandbox_version: 'Puppeteer Headless 1.0'
      }
    });

    // Smart status update based on threat score
    let autoStatus = 'Investigating';
    let autoPriority = undefined;
    if (aiResult.threat_score >= 90) {
      autoPriority = 'Critical';
    } else if (aiResult.threat_score <= 10 && aiResult.verdict === 'Benign') {
      autoStatus = 'Closed'; // Auto-close obvious benign cases
    }

    await prisma.case.update({
      where: { id: c.id },
      data: {
        status: autoStatus,
        ...(autoPriority ? { priority: autoPriority } : {})
      }
    });

    await prisma.auditLog.create({
      data: {
        userId: req.user?.userId || c.userId,
        caseId: c.id,
        action: 'CASE_ANALYZED',
        ip_address: req.ip || null,
        metadata: { threatScore: aiResult.threat_score, severity: severity, targetType: c.target_type }
      }
    });

    res.status(201).json(analysis);
  } catch (err) {
    logger.error('Analysis Engine Error', { error: err.message, stack: err.stack, caseId: req.body.caseId });
    next(err);
  }
};

exports.getAnalysisByCase = async (req, res, next) => {
  try {
    const { caseId } = req.params;
    const analysis = await prisma.analysis.findFirst({
      where: { caseId },
      include: { case: true },
      orderBy: { analyzed_at: 'desc' }
    });
    if (!analysis) return res.status(404).json({ error: 'No analysis found for this case' });
    if (analysis.case.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });
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
      include: { case: true },
      orderBy: { analyzed_at: 'desc' }
    });
    if (!analysis || !analysis.page_screenshot) {
      return res.status(404).json({ error: 'Screenshot not found' });
    }
    if (analysis.case.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

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
