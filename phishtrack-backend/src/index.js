const dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const cors = require('cors');
const logger = require('./utils/logger');
const healthRouter = require('./routes/health.route');
const authRouter = require('./routes/auth.route');
const casesRouter = require('./routes/cases.route');
const dashboardRouter = require('./routes/dashboard.route');
const reportsRouter = require('./routes/reports.route');
const analysisRouter = require('./routes/analysis.route');
const auditRouter = require('./routes/audit.route');
const configRouter = require('./routes/config.route');
const redisClient = require('./redisClient');
const rateLimitMiddleware = require('./middleware/rateLimit.middleware');
const auditMiddleware = require('./middleware/audit.middleware');
const fs = require('fs');
const path = require('path');

// Ensure reports directory exists
const reportsDir = path.join(__dirname, '../uploads/reports');
if (!fs.existsSync(reportsDir)) {
  fs.mkdirSync(reportsDir, { recursive: true });
}

// Startup environment checks
const requiredEnv = []; // Add 'SMTP_HOST', 'SMTP_USER', 'SMTP_PASS' here if you want to strictly enforce it
const missing = requiredEnv.filter(k => !process.env[k] || String(process.env[k]).trim() === '');
if (missing.length) {
  logger.error(`Missing required environment variables: ${missing.join(', ')}.`);
  throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
}

const app = express();
app.use(cors());
app.use(express.json());

// Apply Global Rate Limiting (100 req per 15 mins)
app.use(rateLimitMiddleware({
  windowMs: 15 * 60 * 1000,
  max: 100,
  keyPrefix: 'rl:global:'
}));

// Apply Global Audit Logging for mutations and auth
app.use(auditMiddleware());

app.use('/api', healthRouter);
app.use('/api/auth', authRouter);
app.use('/api/cases', casesRouter);
app.use('/api/dashboard', dashboardRouter);
app.use('/api/reports', reportsRouter);
app.use('/api/analysis', analysisRouter);
app.use('/api/audit', auditRouter);
app.use('/api/config', configRouter);

// global error handler
app.use((err, req, res, next) => {
  logger.error('Unhandled error:', err);
  res.status(500).json({ error: err.message || 'internal server error' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, async () => {
  logger.info(`PhishTrack backend listening on port ${PORT}`);

  // Ensure DB enum has False_Positive (uses direct connection bypassing PgBouncer)
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
