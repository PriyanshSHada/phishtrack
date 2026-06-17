const express = require('express');
const router = express.Router();
const { getVersion } = require('../controllers/config.controller');

router.get('/version', getVersion);

module.exports = router;
