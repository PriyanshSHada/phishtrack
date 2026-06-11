const express = require('express');
const router = express.Router();
const cases = require('../controllers/cases.controller');
const authMiddleware = require('../middleware/auth.middleware');
const { validate, schemas } = require('../middleware/validation.middleware');

// Protect all case routes
router.use(authMiddleware);

router.get('/', cases.getAllCases);
router.post('/', validate(schemas.createCase), cases.createCase);
router.get('/:id', cases.getCaseById);
router.put('/:id', validate(schemas.updateCase), cases.updateCase);
router.delete('/:id', cases.deleteCase);
router.get('/:id/timeline', cases.getCaseTimeline);

module.exports = router;
