require('dotenv').config();
const whoisService = require('./src/services/analysis/whois.service');
const ipgeoService = require('./src/services/analysis/ipgeo.service');
const sslService = require('./src/services/analysis/ssl.service');
const virustotalService = require('./src/services/analysis/virustotal.service');
const domainCheckService = require('./src/services/analysis/domainCheck.service');
const homographService = require('./src/services/analysis/homograph.service');
const puppeteerService = require('./src/services/sandbox/puppeteer.service');
const openaiService = require('./src/services/ai/openai.service');
const pdfService = require('./src/services/report/pdf.service');
const fs = require('fs');

async function main() {
  // Google's Safe Browsing test page for phishing
  const url = 'http://testsafebrowsing.appspot.com/s/phishing.html';
  console.log(`Testing URL: ${url}`);

  console.log('Fetching data concurrently...');
  const [
    whoisResult,
    ipgeoResult,
    sslResult,
    virustotalResult,
    similarityResult,
    homographResult,
    sandboxResult
  ] = await Promise.allSettled([
    whoisService.getWhoisData(url),
    ipgeoService.getIpGeoData(url),
    sslService.getSslInfo(url),
    virustotalService.getVirusTotalResult(url),
    domainCheckService.checkSimilarity(url),
    homographService.detectHomographAttack(url),
    puppeteerService.runSandbox(url)
  ]);

  const analysisData = {
    url: url,
    targetType: 'URL',
    whois: whoisResult.status === 'fulfilled' && !whoisResult.value?.error ? whoisResult.value : null,
    ipGeo: ipgeoResult.status === 'fulfilled' && ipgeoResult.value !== null ? ipgeoResult.value : null,
    ssl: sslResult.status === 'fulfilled' ? sslResult.value : { valid: false, error: 'SSL check failed' },
    virustotal: virustotalResult.status === 'fulfilled' ? virustotalResult.value : null,
    similarity: similarityResult.status === 'fulfilled' ? similarityResult.value : null,
    homograph: homographResult.status === 'fulfilled' ? homographResult.value : null,
    redirectChain: sandboxResult.status === 'fulfilled' ? (sandboxResult.value.redirectChain || [url]) : [url]
  };

  console.log('Data fetched! Running AI analysis...');
  const aiResult = await openaiService.analyzeWithAi(analysisData);

  const fullData = {
    caseData: { target_type: 'URL', url: url, case_number: 'TEST-123', status: 'Closed' },
    analysis: {
      threat_score: aiResult.threat_score,
      confidence: aiResult.confidence,
      severity: aiResult.severity,
      verdict: aiResult.verdict,
      brand_impersonated: aiResult.brand_impersonated,
      whois_data: analysisData.whois,
      ip_geolocation: analysisData.ipGeo,
      ssl_info: analysisData.ssl,
      redirect_chain: analysisData.redirectChain,
      virustotal_result: analysisData.virustotal,
      page_screenshot: sandboxResult.status === 'fulfilled' ? sandboxResult.value.screenshot : null,
      page_source_hash: sandboxResult.status === 'fulfilled' ? sandboxResult.value.pageSourceHash : null,
      ai_summary: aiResult.ai_summary,
      ai_indicators: aiResult.indicators,
      ai_techniques: aiResult.techniques,
      mitre_techniques: aiResult.mitre_techniques
    }
  };

  fs.writeFileSync('test_phishing_report.json', JSON.stringify(fullData, null, 2));
  console.log('Analysis saved to test_phishing_report.json');

  console.log('Generating PDF...');
  const pdfBuffer = await pdfService.generatePdfReport({ case: fullData.caseData, analysis: fullData.analysis });
  fs.writeFileSync('test_phishing_report.pdf', pdfBuffer);
  console.log('PDF saved to test_phishing_report.pdf');
}

main().catch(console.error);
