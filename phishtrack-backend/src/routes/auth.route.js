const express = require('express');
const router = express.Router();
const { register, login, verifyOtp, me, resendOtp } = require('../controllers/auth.controller');
const authMiddleware = require('../middleware/auth.middleware');

const rateLimitMiddleware = require('../middleware/rateLimit.middleware');

const authLimiter = rateLimitMiddleware({
  windowMs: 15 * 60 * 1000,
  max: 10,
  keyPrefix: 'rl:auth:'
});

router.post('/register', authLimiter, register);
router.post('/login', authLimiter, login);
router.post('/verify-otp', authLimiter, verifyOtp);
router.post('/resend-otp', authLimiter, resendOtp);
router.get('/me', authMiddleware, me);

module.exports = router;
