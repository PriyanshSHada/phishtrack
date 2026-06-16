const crypto = require('crypto');
const SECRET = process.env.REPORT_SIGN_SECRET || process.env.JWT_SECRET || 'report-dev-secret';

exports.sign = (payload) => {
  const hmac = crypto.createHmac('sha256', SECRET);
  hmac.update(typeof payload === 'string' ? payload : JSON.stringify(payload));
  return hmac.digest('hex');
};

exports.verify = (payload, signature) => {
  const expected = exports.sign(payload);
  // Use timingSafeEqual to prevent timing-based side-channel attacks
  try {
    return crypto.timingSafeEqual(Buffer.from(expected, 'hex'), Buffer.from(signature, 'hex'));
  } catch {
    return false;
  }
};
