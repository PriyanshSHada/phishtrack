const axios = require('axios');

/**
 * Checks if a host is alive by attempting a quick HTTP connection.
 * Botnet servers often accept TCP connections on random ports but drop the actual HTTP request, 
 * so a short-timeout HEAD/GET request is more accurate than a raw TCP socket.
 * 
 * @param {string} urlStr 
 * @param {number} timeoutMs 
 * @returns {Promise<boolean>}
 */
async function isHostAlive(urlStr, timeoutMs = 2000) {
  try {
    // Some malicious servers drop HEAD requests, so we use a GET request but close it immediately
    // or just rely on the fact that they won't respond in time.
    await axios.get(urlStr, {
      timeout: timeoutMs,
      maxRedirects: 0,
      validateStatus: () => true // Resolve on any HTTP status code
    });
    return true;
  } catch (error) {
    // If it times out or throws connection refused, it's dead
    return false;
  }
}

module.exports = { isHostAlive };
