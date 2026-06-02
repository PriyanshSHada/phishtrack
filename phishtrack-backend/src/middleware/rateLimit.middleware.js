const redisClient = require('../redisClient');

module.exports = (options = {}) => {
  const windowMs = options.windowMs || 15 * 60 * 1000; // Default 15 minutes
  const max = options.max || 100; // Default 100 requests per window
  const keyPrefix = options.keyPrefix || 'rl:';

  return async (req, res, next) => {
    try {
      if (!redisClient.isOpen) {
        // If Redis is not connected, fail-open to avoid service disruption
        return next();
      }

      const identifier = req.user?.userId || req.ip || req.headers['x-forwarded-for'] || 'anonymous';
      const key = `${keyPrefix}${identifier}`;

      // Increment request count in Redis
      const current = await redisClient.incr(key);

      if (current === 1) {
        // Set expiry on first request in the window
        await redisClient.expire(key, Math.ceil(windowMs / 1000));
      }

      const ttl = await redisClient.ttl(key);

      // Set rate limit headers
      res.setHeader('X-RateLimit-Limit', max);
      res.setHeader('X-RateLimit-Remaining', Math.max(0, max - current));
      res.setHeader('X-RateLimit-Reset', new Date(Date.now() + (ttl > 0 ? ttl : 0) * 1000).toISOString());

      if (current > max) {
        return res.status(429).json({
          error: 'Too many requests, please try again later.',
          retryAfter: ttl > 0 ? ttl : 0
        });
      }

      next();
    } catch (err) {
      console.error('Rate limiting middleware error:', err);
      next(); // Fail-open on error
    }
  };
};
