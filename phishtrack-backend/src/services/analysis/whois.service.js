const axios = require('axios');
const logger = require('../../utils/logger');

async function lookupWhois(domain) {
  const whoisModule = await import('whois');
  const whoisClient = whoisModule.default || whoisModule;

  return new Promise((resolve, reject) => {
    whoisClient.lookup(domain, (err, data) => {
      if (err) return reject(err);
      resolve(data);
    });
  });
}

const net = require('net');

exports.getWhoisData = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const domain = urlObj.hostname.replace(/^www\./, '');

    // Skip WHOIS if it's a raw IP address
    if (net.isIP(domain)) {
      return {
        domain: domain,
        raw_text: `WHOIS lookup skipped for raw IP address: ${domain}`,
        creation_date: null,
        expiry_date: null,
        registrar: 'N/A',
        registrant_country: 'N/A',
        isSuspiciousAge: false,
        daysOld: 9999
      };
    }

    let rawData = '';
    let creationDateStr = null;
    let expiryDateStr = null;
    let registrarStr = 'Unknown';
    let countryStr = 'Unknown';

    try {
      // 1. Try API Ninjas WHOIS API
      const apiKey = process.env.API_NINJAS_KEY;
      if (!apiKey) throw new Error('API_NINJAS_KEY is missing');
      
      const res = await axios.get(`https://api.api-ninjas.com/v1/whois?domain=${domain}`, { 
        headers: { 'X-Api-Key': apiKey },
        timeout: 15000 
      });
      
      if (res.data && !res.data.error && Object.keys(res.data).length > 0) {
        rawData = JSON.stringify(res.data);
        
        // API Ninjas returns UNIX timestamps
        if (res.data.creation_date) {
          creationDateStr = new Date(res.data.creation_date * 1000).toISOString();
        }
        if (res.data.expiration_date) {
          expiryDateStr = new Date(res.data.expiration_date * 1000).toISOString();
        }
        if (res.data.registrar) registrarStr = res.data.registrar;
      } else {
        throw new Error('API Ninjas returned empty or invalid status');
      }
    } catch (restErr) {
      logger.warn('API Ninjas WHOIS failed, falling back to raw whois', { error: restErr.message });
      // 2. Fallback to raw port 43 whois lookup
      try {
        rawData = await lookupWhois(domain);
      } catch (whoisErr) {
        logger.warn('Raw WHOIS lookup failed', { error: whoisErr.message, domain });
        rawData = '';
      }
    }
    
    // Parse raw whois text using regex for any missing fields
    const creationMatch = rawData.match(/(?:Creation Date|Created On|created|registered|Registration Time):\s*([^\r\n]+)/i);
    const expiryMatch = rawData.match(/(?:Registry Expiry Date|Expiration Date|Expires On|expires|Expiration Time):\s*([^\r\n]+)/i);
    const registrarMatch = rawData.match(/(?:Registrar|Sponsoring Registrar):\s*([^\r\n]+)/i);
    const countryMatch = rawData.match(/(?:Registrant Country|country):\s*([^\r\n]+)/i);

    if (!creationDateStr && creationMatch) creationDateStr = creationMatch[1].trim();
    if (!expiryDateStr && expiryMatch) expiryDateStr = expiryMatch[1].trim();
    if (registrarStr === 'Unknown' && registrarMatch) registrarStr = registrarMatch[1].trim();
    if (countryStr === 'Unknown' && countryMatch) countryStr = countryMatch[1].trim();

    const creationDate = creationDateStr ? new Date(creationDateStr) : null;
    const expiryDate = expiryDateStr ? new Date(expiryDateStr) : null;

    let ageDays = null;
    let isSuspiciousAge = false;
    if (creationDate && !isNaN(creationDate.getTime())) {
      const diffTime = Math.abs(new Date() - creationDate);
      ageDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      if (ageDays < 30) {
        isSuspiciousAge = true;
      }
    }

    return {
      raw: rawData.substring(0, 1000), // store snippet of raw data
      domain,
      registrar: registrarStr,
      country: countryStr,
      creationDate: creationDate ? creationDate.toISOString() : null,
      expiryDate: expiryDate ? expiryDate.toISOString() : null,
      ageDays,
      isSuspiciousAge
    };
  } catch (err) {
    logger.error('WHOIS Lookup Error', { error: err.message, stack: err.stack, url: urlStr });
    return null; // Return null so analysis doesn't store an error object
  }
};
