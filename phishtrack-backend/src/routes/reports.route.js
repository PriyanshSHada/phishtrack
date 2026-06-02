const express = require('express');
const router = express.Router();
const reports = require('../controllers/reports.controller');
const authMiddleware = require('../middleware/auth.middleware');

// protect report endpoints
router.use(authMiddleware);

router.post('/generate/:caseId', reports.generateReport);
router.post('/generate', reports.generateReport);
router.get('/case/:caseId', reports.getReportByCase);
router.get('/:id', reports.getReport);
router.get('/:id/pdf', reports.downloadPdf);
router.get('/:id/verify', reports.verifyReport);

module.exports = router;
