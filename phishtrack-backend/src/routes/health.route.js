const express = require('express');
const router = express.Router();
const redisClient = require('../redisClient');

router.get('/health', async (req, res) => {
  let redisStatus = 'disconnected';
  try {
    if (redisClient.isOpen) {
      await redisClient.ping();
      redisStatus = 'connected';
    }
  } catch (err) {
    redisStatus = `error: ${err.message}`;
  }

  res.json({ 
    status: 'ok', 
    uptime: process.uptime(),
    redis: redisStatus
  });
});

module.exports = router;
