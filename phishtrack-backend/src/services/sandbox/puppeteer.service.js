const axios = require('axios');
const crypto = require('crypto');
const logger = require('../../utils/logger');

exports.runSandbox = async (url) => {
  try {
    const redirectChain = [url];
    let currentUrl = url;
    let html = '';
    const MAX_REDIRECTS = 10;
    
    // 1. Fetch HTML and manually track redirects
    for (let i = 0; i <= MAX_REDIRECTS; i++) {
      const response = await axios.get(currentUrl, {
        maxRedirects: 0, // Disable automatic redirect following
        validateStatus: status => status >= 200 && status < 400, // Allow 3xx statuses to pass through
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8'
        },
        timeout: 30000
      });

      if (response.status >= 300 && response.status < 400 && response.headers.location) {
        const redirectUrl = new URL(response.headers.location, currentUrl).toString();
        if (!redirectChain.includes(redirectUrl)) {
          redirectChain.push(redirectUrl);
        }
        currentUrl = redirectUrl;
      } else {
        html = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
        break;
      }
    }

    const hash = crypto.createHash('sha256').update(html || '').digest('hex');

    // 2. Get screenshot (Fallback to thum.io if ScreenshotOne API key is missing or fails)
    let screenshotBase64 = null;
    const apiKey = process.env.SCREENSHOTONE_API_KEY;
    
    try {
      if (apiKey) {
        const screenshotApiUrl = `https://api.screenshotone.com/take?access_key=${apiKey}&url=${encodeURIComponent(currentUrl)}&full_page=true&viewport_width=1280&viewport_height=720&format=png`;
        const screenshotRes = await axios.get(screenshotApiUrl, {
          responseType: 'arraybuffer',
          timeout: 35000
        });
        screenshotBase64 = Buffer.from(screenshotRes.data, 'binary').toString('base64');
      } else {
        throw new Error('No ScreenshotOne API key');
      }
    } catch (apiErr) {
      logger.warn('ScreenshotOne failed/missing, falling back to thum.io', { url: currentUrl });
      try {
        const fallbackUrl = `https://image.thum.io/get/width/1280/crop/720/${currentUrl}`;
        const fallbackRes = await axios.get(fallbackUrl, {
          responseType: 'arraybuffer',
          timeout: 35000
        });
        screenshotBase64 = Buffer.from(fallbackRes.data, 'binary').toString('base64');
      } catch (fallbackErr) {
        logger.error('Fallback screenshot failed', { error: fallbackErr.message, url: currentUrl });
      }
    }

    return {
      html,
      pageSourceHash: hash,
      screenshot: screenshotBase64 ? `data:image/png;base64,${screenshotBase64}` : null,
      redirectChain
    };
  } catch (err) {
    logger.error('Sandbox Error', { error: err.message, stack: err.stack, url: url });
    return {
      error: 'Failed to analyze page',
      details: err.message
    };
  }
};
