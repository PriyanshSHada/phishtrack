const express = require('express');
const router = express.Router();
const { register, login, verifyOtp, me, resendOtp } = require('../controllers/auth.controller');
const authMiddleware = require('../middleware/auth.middleware');
const { validate, schemas } = require('../middleware/validation.middleware');
const rateLimitMiddleware = require('../middleware/rateLimit.middleware');

// Stricter per-endpoint rate limits for auth
const loginLimiter = rateLimitMiddleware({
  windowMs: 15 * 60 * 1000,   // 15 minutes
  max: 5,                       // 5 login attempts
  keyPrefix: 'rl:login:'
});

const registerLimiter = rateLimitMiddleware({
  windowMs: 60 * 60 * 1000,    // 1 hour
  max: 3,                       // 3 registrations per hour
  keyPrefix: 'rl:register:'
});

const otpLimiter = rateLimitMiddleware({
  windowMs: 5 * 60 * 1000,     // 5 minutes
  max: 10,                      // 10 OTP verifications
  keyPrefix: 'rl:otp:'
});

const resendOtpLimiter = rateLimitMiddleware({
  windowMs: 5 * 60 * 1000,     // 5 minutes
  max: 2,                       // 2 resends
  keyPrefix: 'rl:resendOtp:'
});

router.post('/register', registerLimiter, validate(schemas.register), register);
router.post('/login', loginLimiter, validate(schemas.login), login);
router.post('/verify-otp', otpLimiter, validate(schemas.verifyOtp), verifyOtp);
router.post('/resend-otp', resendOtpLimiter, validate(schemas.resendOtp), resendOtp);
router.post('/refresh', require('../controllers/auth.controller').refresh);
router.post('/logout', authMiddleware, require('../controllers/auth.controller').logout);
router.get('/me', authMiddleware, me);

module.exports = router;
