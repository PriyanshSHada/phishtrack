const brands = [
  'google.com', 'facebook.com', 'microsoft.com', 'apple.com', 'amazon.com',
  'netflix.com', 'paypal.com', 'yahoo.com', 'linkedin.com', 'instagram.com',
  'twitter.com', 'whatsapp.com', 'dropbox.com', 'github.com', 'adobe.com',
  'zoom.us', 'spotify.com', 'ebay.com', 'chase.com', 'wellsfargo.com',
  'bankofamerica.com', 'citibank.com', 'hdfcbank.com', 'icicibank.com',
  'sbi.co.in', 'paytm.com', 'phonepe.com', 'binance.com', 'coinbase.com',
  'steamcommunity.com', 'epicgames.com', 'roblox.com', 'discord.com',
  'reddit.com', 'wikipedia.org', 'outlook.com', 'live.com', 'office.com'
];

function levenshteinDistance(s1, s2) {
  const m = s1.length;
  const n = s2.length;
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (s1[i - 1] === s2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1];
      } else {
        dp[i][j] = Math.min(
          dp[i - 1][j] + 1,    // deletion
          dp[i][j - 1] + 1,    // insertion
          dp[i - 1][j - 1] + 1 // substitution
        );
      }
    }
  }
  return dp[m][n];
}

exports.checkSimilarity = (urlStr) => {
  try {
    const urlObj = new URL(urlStr);
    const domain = urlObj.hostname.replace(/^www\./, '');

    let matchedBrand = null;
    let minDistance = Infinity;

    for (const brand of brands) {
      // Check edit distance of the SLD (Second Level Domain)
      const brandSld = brand.split('.')[0];
      const domainSld = domain.split('.')[0];

      // Exact match
      if (domain === brand) {
        return { isBrandDomain: true, similarTo: null, distance: 0 };
      }

      // If domain contains brand name but is not the brand (e.g. login-paypal.com)
      if (domain.includes(brandSld) && domain !== brand) {
        return { isBrandDomain: false, similarTo: brand, distance: 1, reason: 'contains_brand_name' };
      }

      const dist = levenshteinDistance(domainSld, brandSld);
      if (dist < minDistance) {
        minDistance = dist;
        matchedBrand = brand;
      }
    }

    // Levenshtein threshold: if edit distance is 1 or 2, flag as suspicious typosquatting
    if (minDistance > 0 && minDistance <= 2) {
      return { isBrandDomain: false, similarTo: matchedBrand, distance: minDistance, reason: 'typosquatting' };
    }

    return { isBrandDomain: false, similarTo: null, distance: minDistance };
  } catch (err) {
    console.error('Similarity check error:', err);
    return { isBrandDomain: false, similarTo: null, error: err.message };
  }
};
