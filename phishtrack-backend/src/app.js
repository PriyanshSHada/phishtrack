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
const rateLimitMiddleware = require('./middleware/rateLimit.middleware');
const auditMiddleware = require('./middleware/audit.middleware');
const fs = require('fs');
const path = require('path');

const reportsDir = path.join(__dirname, '../uploads/reports');
if (!fs.existsSync(reportsDir)) {
  fs.mkdirSync(reportsDir, { recursive: true });
}

const app = express();
const corsOptions = {
  origin: process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : '*'
};
app.use(cors(corsOptions));
app.use(express.json());

app.use(rateLimitMiddleware({
  windowMs: 15 * 60 * 1000,
  max: 100,
  keyPrefix: 'rl:global:'
}));

app.use(auditMiddleware());

app.use('/api', healthRouter);
app.use('/api/auth', authRouter);
app.use('/api/cases', casesRouter);
app.use('/api/dashboard', dashboardRouter);
app.use('/api/reports', reportsRouter);
app.use('/api/analysis', analysisRouter);
app.use('/api/audit', auditRouter);
app.use('/api/config', configRouter);

app.use((err, req, res, next) => {
  logger.error('Unhandled error:', err);
  res.status(500).json({ error: err.message || 'internal server error' });
});

module.exports = app;
