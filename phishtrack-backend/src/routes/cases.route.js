const express = require('express');
const router = express.Router();
const cases = require('../controllers/cases.controller');
const authMiddleware = require('../middleware/auth.middleware');

router.get('/', cases.getAllCases);
router.post('/', cases.createCase);
router.get('/:id', cases.getCaseById);
router.put('/:id', authMiddleware, cases.updateCase);
router.delete('/:id', authMiddleware, cases.deleteCase);
router.get('/:id/timeline', cases.getCaseTimeline);

module.exports = router;
