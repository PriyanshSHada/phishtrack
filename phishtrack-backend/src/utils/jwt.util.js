const jwt = require('jsonwebtoken');
const SECRET = process.env.JWT_SECRET || 'dev-secret';
const EXPIRES_IN = '7d';

exports.signJwt = (payload) => jwt.sign(payload, SECRET, { expiresIn: EXPIRES_IN });

exports.verifyJwt = (token) => {
  try {
    return jwt.verify(token, SECRET);
  } catch (err) {
    return null;
  }
};
