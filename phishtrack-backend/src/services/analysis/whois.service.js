const whois = require('whois');

function lookupWhois(domain) {
  return new Promise((resolve, reject) => {
    whois.lookup(domain, (err, data) => {
      if (err) return reject(err);
      resolve(data);
    });
  });
}

exports.getWhoisData = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const domain = urlObj.hostname.replace(/^www\./, '');

    const rawData = await lookupWhois(domain);
    
    // Parse raw whois text using regex
    const creationMatch = rawData.match(/(?:Creation Date|Created On|created|registered|Registration Time):\s*([^\r\n]+)/i);
    const expiryMatch = rawData.match(/(?:Registry Expiry Date|Expiration Date|Expires On|expires|Expiration Time):\s*([^\r\n]+)/i);
    const registrarMatch = rawData.match(/(?:Registrar|Sponsoring Registrar):\s*([^\r\n]+)/i);
    const countryMatch = rawData.match(/(?:Registrant Country|country):\s*([^\r\n]+)/i);

    const creationDate = creationMatch ? new Date(creationMatch[1].trim()) : null;
    const expiryDate = expiryMatch ? new Date(expiryMatch[1].trim()) : null;

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
      registrar: registrarMatch ? registrarMatch[1].trim() : 'Unknown',
      country: countryMatch ? countryMatch[1].trim() : 'Unknown',
      creationDate: creationDate ? creationDate.toISOString() : null,
      expiryDate: expiryDate ? expiryDate.toISOString() : null,
      ageDays,
      isSuspiciousAge
    };
  } catch (err) {
    console.error('WHOIS Lookup Error:', err);
    return null; // Return null so analysis doesn't store an error object
  }
};
