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

exports.getWhoisData = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const domain = urlObj.hostname.replace(/^www\./, '');

    let rawData = '';
    let creationDateStr = null;
    let expiryDateStr = null;
    let registrarStr = 'Unknown';
    let countryStr = 'Unknown';

    try {
      // 1. Try free REST API (bypasses Render Port 43 block)
      const res = await axios.get(`https://networkcalc.com/api/dns/whois/${domain}`, { timeout: 30000 });
      if (res.data && res.data.status === 'OK' && res.data.whois) {
        rawData = res.data.whois.record || JSON.stringify(res.data.whois);
        
        // Networkcalc parsed fields
        if (res.data.whois.registry_created_date) creationDateStr = res.data.whois.registry_created_date;
        else if (res.data.whois.created) creationDateStr = res.data.whois.created;
        
        if (res.data.whois.registry_expiration_date) expiryDateStr = res.data.whois.registry_expiration_date;
        else if (res.data.whois.expires) expiryDateStr = res.data.whois.expires;
        
        if (res.data.whois.registrar) registrarStr = res.data.whois.registrar;
        if (res.data.whois.registrant_country) countryStr = res.data.whois.registrant_country;
      } else {
        throw new Error('REST API returned empty or invalid status');
      }
    } catch (restErr) {
      logger.warn('NetworkCalc WHOIS failed, falling back to raw whois', { error: restErr.message });
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
