const tls = require('tls');

function getSslDetails(hostname) {
  return new Promise((resolve, reject) => {
    const socket = tls.connect(443, hostname, { servername: hostname, rejectUnauthorized: false }, () => {
      const cert = socket.getPeerCertificate(true);
      socket.end();
      if (!cert || Object.keys(cert).length === 0) {
        return resolve({ valid: false, error: 'No certificate found' });
      }
      resolve(cert);
    });
    // Set timeout before the connection completes so it guards against hung connects
    socket.setTimeout(5000);
    socket.on('timeout', () => {
      socket.destroy();
      resolve({ valid: false, error: 'Connection timed out' });
    });
    socket.on('error', (err) => {
      resolve({ valid: false, error: err.message });
    });
  });
}

exports.getSslInfo = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    if (urlObj.protocol !== 'https:') {
      return { valid: false, error: 'Protocol is not HTTPS' };
    }
    const hostname = urlObj.hostname;
    const cert = await getSslDetails(hostname);
    if (cert.error) {
      return { valid: false, error: cert.error };
    }

    const validFrom = new Date(cert.valid_from);
    const validTo = new Date(cert.valid_to);
    const now = new Date();
    const isExpired = now < validFrom || now > validTo;

    return {
      valid: !isExpired,
      subject: cert.subject,
      issuer: cert.issuer,
      validFrom: validFrom.toISOString(),
      validTo: validTo.toISOString(),
      fingerprint: cert.fingerprint,
      serialNumber: cert.serialNumber
    };
  } catch (err) {
    console.error('SSL Check Error:', err);
    return { valid: false, error: err.message || 'SSL verification failed' };
  }
};
