const dns = require('dns').promises;
const logger = require('../../utils/logger');

async function fetchIpGeo(ip) {
  const res = await fetch(`http://ip-api.com/json/${ip}`);
  if (!res.ok) {
    throw new Error('IP Geolocation service returned an error');
  }
  const data = await res.json();

  if (data.status !== 'success') {
    logger.warn('IP Geo: ip-api returned non-success status', { ip, message: data.message || 'unknown' });
    return null;
  }

  return {
    ip,
    country: data.country || 'Unknown',
    countryCode: data.countryCode || 'Unknown',
    region: data.regionName || 'Unknown',
    city: data.city || 'Unknown',
    zip: data.zip || 'Unknown',
    lat: data.lat || 0,
    lon: data.lon || 0,
    isp: data.isp || 'Unknown',
    org: data.org || 'Unknown',
    as: data.as || 'Unknown'
  };
}

exports.getIpGeoDataFromIp = async (ip) => {
  try {
    return await fetchIpGeo(ip);
  } catch (err) {
    logger.error('IP Geo Error', { error: err.message, stack: err.stack, ip });
    return null;
  }
};

exports.getIpGeoData = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const hostname = urlObj.hostname;

    const addresses = await dns.resolve4(hostname);
    if (!addresses || addresses.length === 0) {
      throw new Error('Could not resolve host to IP');
    }
    const ip = addresses[0];

    return await fetchIpGeo(ip);
  } catch (err) {
    logger.error('IP Geo Error', { error: err.message, stack: err.stack, url: urlStr });
    return null; // Return null instead of error string so threat map doesn't crash on this record
  }
};
