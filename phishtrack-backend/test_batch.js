require('dotenv').config();
const fs = require('fs');
const path = require('path');
const whoisService = require('./src/services/analysis/whois.service');
const ipgeoService = require('./src/services/analysis/ipgeo.service');
const sslService = require('./src/services/analysis/ssl.service');
const virustotalService = require('./src/services/analysis/virustotal.service');
const domainCheckService = require('./src/services/analysis/domainCheck.service');
const homographService = require('./src/services/analysis/homograph.service');
const puppeteerService = require('./src/services/sandbox/puppeteer.service');
const openaiService = require('./src/services/ai/openai.service');

async function testLinks() {
  const fileContent = fs.readFileSync('realphishing.text', 'utf-8');
  const links = fileContent.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  
  console.log(`Starting test on ${links.length} malicious links...\n`);
  
  const results = [];
  
  for (let i = 0; i < links.length; i++) {
    const url = links[i];
    console.log(`[${i+1}/${links.length}] Analyzing: ${url}`);
    
    try {
      const fetchPromise = Promise.allSettled([
        whoisService.getWhoisData(url),
        ipgeoService.getIpGeoData(url),
        sslService.getSslInfo(url),
        virustotalService.getVirusTotalResult(url),
        domainCheckService.checkSimilarity(url),
        homographService.detectHomographAttack(url),
        puppeteerService.runSandbox(url)
      ]);
      
      const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error('Timeout after 15 seconds')), 15000));
      
      const [
        whoisResult,
        ipgeoResult,
        sslResult,
        virustotalResult,
        similarityResult,
        homographResult,
        sandboxResult
      ] = await Promise.race([fetchPromise, timeoutPromise]);

      const analysisData = {
        url: url,
        targetType: url.includes('://') && !isNaN(url.split('://')[1].split(':')[0].replace(/\./g, '')) ? 'IP' : 'URL', // Simple IP/URL check
        whois: whoisResult.status === 'fulfilled' && !whoisResult.value?.error ? whoisResult.value : null,
        ipGeo: ipgeoResult.status === 'fulfilled' && ipgeoResult.value !== null ? ipgeoResult.value : null,
        ssl: sslResult.status === 'fulfilled' ? sslResult.value : { valid: false, error: 'SSL check failed' },
        virustotal: virustotalResult.status === 'fulfilled' ? virustotalResult.value : null,
        similarity: similarityResult.status === 'fulfilled' ? similarityResult.value : null,
        homograph: homographResult.status === 'fulfilled' ? homographResult.value : null,
        redirectChain: sandboxResult.status === 'fulfilled' && sandboxResult.value.redirectChain ? sandboxResult.value.redirectChain : [url]
      };
      
      console.log('   -> Data fetched, running AI analysis...');
      const aiResult = await openaiService.analyzeWithAi(analysisData);
      
      const resultObj = {
        url: url,
        virustotal_detections: analysisData.virustotal?.maliciousCount || 0,
        whois_suspicious_age: analysisData.whois?.isSuspiciousAge || false,
        ai_threat_score: aiResult.threat_score,
        ai_verdict: aiResult.verdict,
        ai_severity: aiResult.severity,
        ai_confidence: aiResult.confidence
      };
      
      results.push(resultObj);
      console.log(`   -> Finished. AI Verdict: ${aiResult.verdict} (Score: ${aiResult.threat_score})\n`);
      
    } catch (e) {
      console.error(`   -> Error processing ${url}:`, e.message);
    }
  }
  
  console.log('--- TEST RESULTS SUMMARY ---');
  console.table(results);
  
  fs.writeFileSync('batch_phishing_test_results.json', JSON.stringify(results, null, 2));
}

testLinks().catch(console.error);
