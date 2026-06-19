const prisma = require('../prismaClient');
const { hashPassword, comparePassword } = require('../utils/hash.util');
const { signAccessToken, signRefreshToken, blacklistToken, verifyJwt } = require('../utils/jwt.util');
const emailService = require('../services/email.service');
const redisClient = require('../redisClient');
const logger = require('../utils/logger');

exports.register = async (req, res, next) => {
  try {
    const { email, password, name, organization } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Missing fields' });
    const emailRegex = /^\S+@\S+\.\S+$/;
    if (!emailRegex.test(email)) return res.status(400).json({ error: 'Invalid email format' });
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) return res.status(409).json({ error: 'User already exists' });
    const passwordHash = await hashPassword(password);
    const user = await prisma.user.create({
      data: { email, password: passwordHash, name, organization, is_verified: true }
    });
    res.status(201).json({
      id: user.id,
      email: user.email,
      name: user.name
    });
  } catch (err) {
    logger.error('Registration error', { error: err.message, stack: err.stack });
    next(err);
  }
};

exports.login = async (req, res, next) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Missing fields' });
    const emailRegex = /^\S+@\S+\.\S+$/;
    if (!emailRegex.test(email)) return res.status(400).json({ error: 'Invalid email format' });
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(401).json({ error: 'Invalid credentials' });
    const ok = await comparePassword(password, user.password);
    if (!ok) return res.status(401).json({ error: 'Invalid credentials' });

    // Generate 6-digit OTP code
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    // Store in Redis (expiration: 5 minutes = 300 seconds)
    if (redisClient.isOpen) {
      await redisClient.setEx(`otp:${email}`, 300, otp);
    } else {
      logger.error('Redis client is not open; unable to store OTP.');
      throw new Error('Redis connection error');
    }

    // Send OTP email
    await emailService.sendOtp(email, otp);

    res.json({ message: 'OTP sent to email', email: user.email });
  } catch (err) {
    logger.error('Login error', { error: err.message, stack: err.stack });
    next(err);
  }
};

exports.verifyOtp = async (req, res, next) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp) return res.status(400).json({ error: 'Missing fields (email and otp required)' });

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(404).json({ error: 'User not found' });

    if (!redisClient.isOpen) {
      return res.status(500).json({ error: 'Redis connection unavailable' });
    }

    const cachedOtp = await redisClient.get(`otp:${email}`);
    if (!cachedOtp) return res.status(400).json({ error: 'OTP expired or not found' });
    if (cachedOtp !== otp) return res.status(400).json({ error: 'Invalid OTP' });

    // Clean up OTP from Redis
    await redisClient.del(`otp:${email}`);

    if (!user.is_verified) {
      await prisma.user.update({ where: { id: user.id }, data: { is_verified: true } });
    }

    const token = signAccessToken({ userId: user.id });
    const refreshToken = signRefreshToken({ userId: user.id });
    res.json({ token, refreshToken, user: { id: user.id, email: user.email } });
  } catch (err) {
    logger.error('Verify OTP error', { error: err.message, stack: err.stack });
    next(err);
  }
};

exports.me = async (req, res, next) => {
  try {
    const payload = req.user;
    if (!payload || !payload.userId) return res.status(401).json({ error: 'Not authenticated' });
    const user = await prisma.user.findUnique({ where: { id: payload.userId } });
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json({
      id: user.id,
      email: user.email,
      name: user.name,
      organization: user.organization,
      role: user.role
    });
  } catch (err) {
    logger.error('Get profile error', { error: err.message, stack: err.stack });
    next(err);
  }
};

exports.resendOtp = async (req, res, next) => {
  try {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Missing email' });

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(404).json({ error: 'User not found' });

    // Generate 6-digit OTP code
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    // Store in Redis (expiration: 5 minutes = 300 seconds)
    if (redisClient.isOpen) {
      await redisClient.setEx(`otp:${email}`, 300, otp);
    } else {
      logger.error('Redis client is not open; unable to store OTP.');
      throw new Error('Redis connection error');
    }

    // Send OTP email
    await emailService.sendOtp(email, otp);

    res.json({ message: 'OTP resent to email' });
  } catch (err) {
    logger.error('Resend OTP error', { error: err.message, stack: err.stack });
    next(err);
  }
};

exports.refresh = async (req, res, next) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) return res.status(400).json({ error: 'Refresh token required' });
    
    let payload;
    try {
      payload = await verifyJwt(refreshToken);
    } catch (e) {
      return res.status(401).json({ error: 'Invalid or expired refresh token' });
    }
    
    if (!payload || !payload.userId) return res.status(401).json({ error: 'Invalid or expired refresh token' });
    
    const user = await prisma.user.findUnique({ where: { id: payload.userId } });
    if (!user) return res.status(404).json({ error: 'User not found' });
    
    const newToken = signAccessToken({ userId: user.id });
    const newRefreshToken = signRefreshToken({ userId: user.id });
    
    const decoded = require('jsonwebtoken').decode(refreshToken);
    if (decoded && decoded.exp) {
      const expiresInSecs = decoded.exp - Math.floor(Date.now() / 1000);
      await blacklistToken(refreshToken, expiresInSecs);
    }
    
    res.json({ token: newToken, refreshToken: newRefreshToken });
  } catch (err) {
    next(err);
  }
};

exports.logout = async (req, res, next) => {
  try {
    const auth = req.headers.authorization;
    if (auth && auth.startsWith('Bearer ')) {
      const token = auth.split(' ')[1];
      const decoded = require('jsonwebtoken').decode(token);
      if (decoded && decoded.exp) {
        const expiresInSecs = decoded.exp - Math.floor(Date.now() / 1000);
        await blacklistToken(token, expiresInSecs);
      }
    }
    const { refreshToken } = req.body;
    if (refreshToken) {
      const decodedRefresh = require('jsonwebtoken').decode(refreshToken);
      if (decodedRefresh && decodedRefresh.exp) {
        const expiresInSecs = decodedRefresh.exp - Math.floor(Date.now() / 1000);
        await blacklistToken(refreshToken, expiresInSecs);
      }
    }
    res.json({ message: 'Logged out successfully' });
  } catch (err) {
    next(err);
  }
};
