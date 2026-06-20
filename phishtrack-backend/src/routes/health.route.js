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

router.get('/logs', (req, res) => {
  const fs = require('fs');
  try {
    const logs = fs.readFileSync('logs/error.log', 'utf8');
    const lines = logs.split('\n').filter(Boolean);
    res.json({ logs: lines.slice(-20) }); // last 20 errors
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
