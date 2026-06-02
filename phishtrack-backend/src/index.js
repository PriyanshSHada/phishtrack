const dotenv = require('dotenv');
dotenv.config();

const express = require('express');
const healthRouter = require('./routes/health.route');
const authRouter = require('./routes/auth.route');
const casesRouter = require('./routes/cases.route');
const dashboardRouter = require('./routes/dashboard.route');
const reportsRouter = require('./routes/reports.route');
const analysisRouter = require('./routes/analysis.route');
const auditRouter = require('./routes/audit.route');
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
const requiredEnv = ['RESEND_API_KEY', 'RESEND_FROM'];
const missing = requiredEnv.filter(k => !process.env[k] || String(process.env[k]).trim() === '');
if (missing.length) {
  console.error(`Missing required environment variables: ${missing.join(', ')}.`);
  throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
}

const app = express();
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

// global error handler
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: err.message || 'internal server error' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, async () => {
  console.log(`PhishTrack backend listening on port ${PORT}`);
  try {
    await redisClient.connect();
    console.log('Successfully connected to Redis');
  } catch (err) {
    console.error('Failed to connect to Redis on startup:', err);
  }
});

module.exports = app;
