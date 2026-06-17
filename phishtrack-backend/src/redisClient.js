const { createClient } = require('redis');
const logger = require('./utils/logger');

const redisUrl = process.env.REDIS_URL;
if (!redisUrl) {
  logger.warn('Warning: REDIS_URL environment variable is not defined.');
}

const redisClient = createClient({
  url: redisUrl
});

redisClient.on('error', (err) => {
  logger.error('Redis Client Error', { error: err.message, stack: err.stack });
});

module.exports = redisClient;
