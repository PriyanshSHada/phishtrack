const { domainToASCII, domainToUnicode } = require('url');

const TARGET_BRANDS = ['google', 'facebook', 'amazon', 'paypal', 'microsoft', 'apple', 'instagram', 'netflix'];

// A simple Levenshtein distance function
function levenshtein(a, b) {
  if (a.length === 0) return b.length;
  if (b.length === 0) return a.length;

  const matrix = Array(b.length + 1).fill(null).map(() => Array(a.length + 1).fill(null));

  for (let i = 0; i <= a.length; i += 1) {
    matrix[0][i] = i;
  }
  for (let j = 0; j <= b.length; j += 1) {
    matrix[j][0] = j;
  }

  for (let j = 1; j <= b.length; j += 1) {
    for (let i = 1; i <= a.length; i += 1) {
      const indicator = a[i - 1] === b[j - 1] ? 0 : 1;
      matrix[j][i] = Math.min(
        matrix[j][i - 1] + 1, // deletion
        matrix[j - 1][i] + 1, // insertion
        matrix[j - 1][i - 1] + indicator // substitution
      );
    }
  }
  return matrix[b.length][a.length];
}

// Replaces confusing characters with their latin equivalents
function normalizeLookalikes(str) {
  const lookalikes = {
    'а': 'a', 'о': 'o', 'с': 'c', 'е': 'e', 'р': 'p', 'х': 'x', 'у': 'y', 'і': 'i', 'ј': 'j', 'ѕ': 's', 'ԁ': 'd', 'ԛ': 'q', 'ѡ': 'w'
  };
  return str.split('').map(char => lookalikes[char] || char).join('');
}

exports.detectHomographAttack = async (urlStr) => {
  try {
    let hostname = '';
    try {
      const urlObj = new URL(urlStr);
      hostname = urlObj.hostname;
    } catch (e) {
      // Fallback if URL is missing protocol
      const dummyUrl = new URL('http://' + urlStr);
      hostname = dummyUrl.hostname;
    }
    
    // Extract base domain name without TLD (naive, but sufficient for top brands)
    const parts = hostname.split('.');
    const domainName = parts.length >= 2 ? parts[parts.length - 2] : parts[0];

    const asciiDomain = domainToASCII(hostname);
    const unicodeDomain = domainToUnicode(hostname);

    const hasPunycode = asciiDomain.includes('xn--');
    
    // Check for mixed scripts
    const hasLatin = /\p{Script=Latin}/u.test(unicodeDomain);
    const hasCyrillic = /\p{Script=Cyrillic}/u.test(unicodeDomain);
    const hasGreek = /\p{Script=Greek}/u.test(unicodeDomain);

    let detectedScript = 'Latin';
    if (hasPunycode) {
      if (hasLatin && hasCyrillic) detectedScript = 'Mixed (Latin+Cyrillic)';
      else if (hasLatin && hasGreek) detectedScript = 'Mixed (Latin+Greek)';
      else if (hasCyrillic) detectedScript = 'Cyrillic';
      else if (hasGreek) detectedScript = 'Greek';
      else detectedScript = 'Other Unicode';
    }

    const isMixed = hasLatin && (hasCyrillic || hasGreek);

    // Normalize unicode characters to latin equivalent
    const normalizedName = normalizeLookalikes(domainToUnicode(domainName));
    
    let isHomograph = false;
    let confidence = 'low';
    let similarTo = null;

    // Check similarity with target brands
    for (const brand of TARGET_BRANDS) {
      // Direct spoof with normalized string
      if (normalizedName === brand && hasPunycode) {
        isHomograph = true;
        confidence = 'high';
        similarTo = brand;
        break;
      }

      // Check edit distance on normalized
      const distance = levenshtein(normalizedName, brand);
      if (distance <= 1 && distance > 0) {
        isHomograph = true;
        // if it uses punycode to be 1 char off, it's very suspicious
        confidence = hasPunycode ? 'high' : 'medium'; 
        similarTo = brand;
        break;
      }
    }
    
    // Strong indicator: mixed script
    if (isMixed && isHomograph) {
      confidence = 'high';
    } else if (isMixed && !isHomograph) {
      // Still a strong indicator of homograph even if not in our top list
      isHomograph = true;
      confidence = 'medium';
      similarTo = 'Unknown Brand';
    }

    return {
      isHomograph,
      confidence: isHomograph ? confidence : 'low',
      detectedScript,
      similarTo: isHomograph ? similarTo : null
    };
  } catch (error) {
    return {
      isHomograph: false,
      confidence: 'low',
      detectedScript: 'Error',
      similarTo: null,
      error: error.message
    };
  }
};
