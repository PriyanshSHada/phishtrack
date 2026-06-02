const express = require('express');
const router = express.Router();
const dashboard = require('../controllers/dashboard.controller');
const authMiddleware = require('../middleware/auth.middleware');

// protect dashboard endpoints
router.use(authMiddleware);

router.get('/stats', dashboard.getStats);
router.get('/recent', dashboard.getRecentCases);
router.get('/weekly', dashboard.getWeeklyGraph);

module.exports = router;
