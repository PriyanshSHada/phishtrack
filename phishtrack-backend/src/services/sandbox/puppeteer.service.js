const puppeteer = require('puppeteer-core');
const crypto = require('crypto');

exports.runSandbox = async (url) => {
  let browser;
  try {
    browser = await puppeteer.launch({
      executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const page = await browser.newPage();
    await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36');
    await page.setViewport({ width: 1280, height: 720 });

    const redirectChain = [];
    page.on('response', response => {
      const status = response.status();
      if (status >= 300 && status <= 399) {
        redirectChain.push(response.url());
      }
    });

    // Go to URL
    await page.goto(url, { waitUntil: 'networkidle2', timeout: 10000 });
    
    // Final URL is also part of redirect chain if it changed
    const finalUrl = page.url();
    if (redirectChain.length === 0 || redirectChain[redirectChain.length - 1] !== finalUrl) {
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
    console.error('Puppeteer Sandbox Error:', err);
    throw err;
  } finally {
    if (browser) {
      await browser.close();
    }
  }
};
