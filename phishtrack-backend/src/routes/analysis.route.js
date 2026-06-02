const express = require('express');
const router = express.Router();
const analysis = require('../controllers/analysis.controller');
const authMiddleware = require('../middleware/auth.middleware');
const rateLimitMiddleware = require('../middleware/rateLimit.middleware');

const analysisLimiter = rateLimitMiddleware({
  windowMs: 24 * 60 * 60 * 1000, // 24 hours
  max: 10, // max 10 requests per day
  keyPrefix: 'rl:analysis:'
});

router.post('/run', authMiddleware, analysisLimiter, analysis.runAnalysis);
router.get('/:caseId', authMiddleware, analysis.getAnalysisByCase);
router.get('/:caseId/screenshot', authMiddleware, analysis.getScreenshot);

module.exports = router;
