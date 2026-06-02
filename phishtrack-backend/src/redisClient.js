const { createClient } = require('redis');

const redisUrl = process.env.REDIS_URL;
if (!redisUrl) {
  console.warn('Warning: REDIS_URL environment variable is not defined.');
}

const redisClient = createClient({
  url: redisUrl
});

redisClient.on('error', (err) => {
  console.error('Redis Client Error:', err);
});

module.exports = redisClient;
