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
        timeout: 15000
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

    // 2. Get screenshot from ScreenshotOne API
    const apiKey = process.env.SCREENSHOTONE_API_KEY;
    if (!apiKey) {
      throw new Error('SCREENSHOTONE_API_KEY is not defined in environment variables');
    }

    const screenshotApiUrl = `https://api.screenshotone.com/take?access_key=${apiKey}&url=${encodeURIComponent(currentUrl)}&full_page=true&viewport_width=1280&viewport_height=720&format=png`;
    
    const screenshotRes = await axios.get(screenshotApiUrl, {
      responseType: 'arraybuffer',
      timeout: 30000
    });

    const screenshotBase64 = Buffer.from(screenshotRes.data, 'binary').toString('base64');

    return {
      html,
      pageSourceHash: hash,
      screenshot: `data:image/png;base64,${screenshotBase64}`,
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
