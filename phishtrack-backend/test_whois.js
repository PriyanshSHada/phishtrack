const whoisService = require('./src/services/analysis/whois.service');
require('dotenv').config();
whoisService.getWhoisData('https://example.com').then(console.log).catch(console.error);
