const dotenv = require('dotenv');
dotenv.config();

const logger = require('./utils/logger');
const redisClient = require('./redisClient');
const app = require('./app');

const requiredEnv = [];
const missing = requiredEnv.filter(k => !process.env[k] || String(process.env[k]).trim() === '');
if (missing.length) {
  logger.error(`Missing required environment variables: ${missing.join(', ')}.`);
  throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, async () => {
  logger.info(`PhishTrack backend listening on port ${PORT}`);

  try {
    const directUrl = process.env.DATABASE_URL.replace('pgbouncer=true', 'pgbouncer=false');
    const { PrismaClient } = require('@prisma/client');
    const prismaEnsure = new PrismaClient({ datasources: { db: { url: directUrl } } });
    await prismaEnsure.$executeRawUnsafe(`ALTER TYPE "Status" ADD VALUE IF NOT EXISTS 'False_Positive'`);
    await prismaEnsure.$disconnect();
    logger.info('DB enum: False_Positive ensured');
  } catch (_) {
    // Value already exists or blocked — safe to continue
  }

  try {
    await redisClient.connect();
    logger.info('Successfully connected to Redis');
  } catch (err) {
    logger.error('Failed to connect to Redis on startup:', err);
  }
});

module.exports = app;
