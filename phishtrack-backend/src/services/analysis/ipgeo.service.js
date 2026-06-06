const dns = require('dns').promises;

exports.getIpGeoData = async (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const hostname = urlObj.hostname;
    
    const addresses = await dns.resolve4(hostname);
    if (!addresses || addresses.length === 0) {
      throw new Error('Could not resolve host to IP');
    }
    const ip = addresses[0];

    const res = await fetch(`http://ip-api.com/json/${ip}`);
    if (!res.ok) {
      throw new Error('IP Geolocation service returned an error');
    }
    const data = await res.json();

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
  } catch (err) {
    console.error('IP Geo Error:', err);
    return null; // Return null instead of error string so threat map doesn't crash on this record
  }
};
