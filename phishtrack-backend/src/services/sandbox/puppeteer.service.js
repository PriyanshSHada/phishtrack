const puppeteer = require('puppeteer-core');
const crypto = require('crypto');
const logger = require('../../utils/logger');
const os = require('os');

function getBrowserExecutablePath() {
  // Render / Linux environments: Chromium installed via buildpack
  if (os.platform() === 'linux') {
    return process.env.CHROMIUM_PATH || '/usr/bin/chromium-browser';
  }
  // Windows dev environment: uses Edge
  return 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
}

exports.runSandbox = async (url) => {
  let browser;
  try {
    browser = await puppeteer.launch({
      executablePath: getBrowserExecutablePath(),
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const page = await browser.newPage();
    await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36');
    await page.setViewport({ width: 1280, height: 720 });

    const redirectChain = [url]; // Start with original URL
    
    // Track navigation requests to capture full redirect chain
    page.on('request', request => {
      if (request.isNavigationRequest()) {
        const reqChain = request.redirectChain();
        reqChain.forEach(reqUrl => {
          if (!redirectChain.includes(reqUrl.url())) {
            redirectChain.push(reqUrl.url());
          }
        });
      }
    });

    // Go to URL
    await page.goto(url, { waitUntil: 'networkidle2', timeout: 15000 });
    
    // Final URL is also part of chain
    const finalUrl = page.url();
    if (!redirectChain.includes(finalUrl)) {
      redirectChain.push(finalUrl);
    }

    const html = await page.content();
    const hash = crypto.createHash('sha256').update(html).digest('hex');
    const screenshotBase64 = await page.screenshot({ encoding: 'base64', fullPage: true });

    return {
      html,
      pageSourceHash: hash,
      screenshot: `data:image/png;base64,${screenshotBase64}`,
      redirectChain
    };
  } catch (err) {
    logger.error('Puppeteer Sandbox Error', { error: err.message, stack: err.stack, url: url });
    return {
      error: 'Failed to analyze page',
      details: err.message
    };
  } finally {
    if (browser) {
      await browser.close();
    }
  }
};
