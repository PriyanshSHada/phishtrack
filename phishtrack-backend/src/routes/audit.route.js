const express = require('express');
const router = express.Router();
const audit = require('../controllers/audit.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.use(authMiddleware);

router.get('/logs', audit.getAuditLogs);
router.get('/custody/:caseId', audit.getCustodyChain);

module.exports = router;
