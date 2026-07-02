const dotenv = require('dotenv');
dotenv.config();

const logger = require('./utils/logger');
const redisClient = require('./redisClient');
const app = require('./app');

// Prevent silent crashes from unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', { promise, reason: reason?.message || reason });
});

process.on('uncaughtException', (err) => {
  logger.error('Uncaught Exception:', { error: err.message, stack: err.stack });
});

const requiredEnv = [];
const missing = requiredEnv.filter(k => !process.env[k] || String(process.env[k]).trim() === '');
if (missing.length) {
  logger.error(`Missing required environment variables: ${missing.join(', ')}.`);
  throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
}

const PORT = process.env.PORT || 3000;

try {
  const { execSync } = require('child_process');
  logger.info('Running Prisma DB Push before startup to force schema sync...');
  const directUrl = process.env.DIRECT_URL || process.env.DATABASE_URL.replace('6543', '5432').replace('pgbouncer=true', 'pgbouncer=false');
  execSync('npx prisma db push --accept-data-loss', { 
    stdio: 'inherit',
    env: { ...process.env, DATABASE_URL: directUrl }
  });
  logger.info('Prisma DB Push completed successfully.');
} catch (err) {
  logger.error('Failed to run Prisma Migrations:', err);
}

app.listen(PORT, async () => {
  logger.info(`PhishTrack backend listening on port ${PORT}`);

  try {
    // Use DIRECT_URL (port 5432) for DDL operations — PgBouncer blocks ALTER TYPE
    const directUrl = process.env.DIRECT_URL || process.env.DATABASE_URL.replace('6543', '5432').replace('pgbouncer=true', 'pgbouncer=false');
    const { PrismaClient } = require('@prisma/client');
    const prismaEnsure = new PrismaClient({ datasources: { db: { url: directUrl } } });
    await prismaEnsure.$executeRawUnsafe(`ALTER TYPE "Status" ADD VALUE IF NOT EXISTS 'False_Positive'`);
    await prismaEnsure.$disconnect();
    logger.info('DB enum: False_Positive ensured');
  } catch (err) {
    logger.warn('DB enum check skipped (may already exist):', { error: err.message });
  }

  try {
    await redisClient.connect();
    logger.info('Successfully connected to Redis');
  } catch (err) {
    logger.error('Failed to connect to Redis on startup:', err);
  }
});

module.exports = app;
