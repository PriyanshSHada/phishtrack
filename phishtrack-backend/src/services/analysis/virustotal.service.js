exports.getVirusTotalResult = async (urlStr) => {
  const logger = require('../../utils/logger');
  const apiKey = process.env.VIRUSTOTAL_API_KEY;
  if (!apiKey) {
    logger.warn('VirusTotal API Key not configured');
    return { error: 'VirusTotal API key not configured' };
  }

  try {
    // Normalize URL: lowercase scheme and host to ensure consistent VT cache hits
    let normalizedUrl = urlStr;
    try {
      const parsed = new URL(urlStr);
      parsed.protocol = parsed.protocol.toLowerCase();
      parsed.hostname = parsed.hostname.toLowerCase();
      normalizedUrl = parsed.toString();
    } catch (_) {
      // If URL parsing fails, fall back to the original string
    }

    // Generate URL identifier: base64 without padding, url-safe
    const urlId = Buffer.from(normalizedUrl).toString('base64')
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');

    const response = await fetch(`https://www.virustotal.com/api/v3/urls/${urlId}`, {
      headers: {
        'x-apikey': apiKey
      }
    });

    if (response.status === 404) {
      // Not found, let's submit for scan
      const scanResponse = await fetch('https://www.virustotal.com/api/v3/urls', {
        method: 'POST',
        headers: {
          'x-apikey': apiKey,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ url: urlStr })
      });
      
      if (!scanResponse.ok) {
        throw new Error('Failed to submit URL to VirusTotal');
      }
      const scanData = await scanResponse.json();
      return {
        scanned: false,
        message: 'URL submitted for scanning. Analysis pending.',
        scanId: scanData.data?.id
      };
    }

    if (!response.ok) {
      throw new Error(`VirusTotal API responded with status ${response.status}`);
    }

    const data = await response.json();
    const stats = data.data?.attributes?.last_analysis_stats || {};
    const results = data.data?.attributes?.last_analysis_results || {};

    // Filter out engines that detected it as malicious
    const detections = Object.entries(results)
      .filter(([_, res]) => res.category === 'malicious' || res.category === 'suspicious')
      .map(([engineName, res]) => ({
        engine: engineName,
        category: res.category,
        result: res.result
      }));

    return {
      scanned: true,
      maliciousCount: stats.malicious || 0,
      suspiciousCount: stats.suspicious || 0,
      harmlessCount: stats.harmless || 0,
      undetectedCount: stats.undetected || 0,
      detections
    };
  } catch (err) {
    logger.error('VirusTotal API Error', { error: err.message, stack: err.stack, url: urlStr });
    return { error: err.message || 'VirusTotal check failed' };
  }
};
