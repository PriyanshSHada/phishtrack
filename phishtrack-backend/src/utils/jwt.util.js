const jwt = require('jsonwebtoken');

const SECRET = process.env.JWT_SECRET;
if (!SECRET) {
  throw new Error('JWT_SECRET environment variable is required');
}

const ACCESS_TOKEN_EXPIRY = '15m';
const REFRESH_TOKEN_EXPIRY = '7d';

exports.signAccessToken = (payload) => jwt.sign(payload, SECRET, { expiresIn: ACCESS_TOKEN_EXPIRY });

exports.signRefreshToken = (payload) => jwt.sign(payload, SECRET, { expiresIn: REFRESH_TOKEN_EXPIRY });

exports.verifyJwt = (token) => {
  try {
    return jwt.verify(token, SECRET);
  } catch (err) {
    return null;
  }
};

// Backward-compatible alias: tokens used to be signed with 7d expiry;
// new code should use signAccessToken/signRefreshToken explicitly.
exports.signJwt = (payload) => jwt.sign(payload, SECRET, { expiresIn: REFRESH_TOKEN_EXPIRY });
