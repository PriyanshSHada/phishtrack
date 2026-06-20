const logger = require('../../utils/logger');

exports.analyzeWithAi = async (analysisData) => {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    logger.warn('OpenAI API key not configured');
    return {
      threat_score: 50,
      severity: 'Medium',
      confidence: 30,
      indicators: ['OpenAI API Key not configured — manual review required'],
      techniques: [],
      mitre_techniques: [],
      ai_summary: 'AI analysis could not run because the OpenAI API key is missing. Threat score defaults to 50. Please review the forensic artifacts manually.'
    };
  }

  try {
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'gpt-4o',
        response_format: { type: 'json_object' },
        messages: [
          {
            role: 'system',
            content: `You are a Senior Forensic Analyst specializing in phishing, malware distribution, and web-based cyber attacks. Your job is to analyze forensic evidence gathered from a suspicious URL or IP and produce a structured, expert-level threat assessment.

Your output MUST be a valid JSON object with EXACTLY these fields:

- "threat_score": integer 0-100. Be calibrated: 0-29 = benign, 30-49 = suspicious, 50-69 = likely malicious, 70-89 = high confidence malicious, 90-100 = confirmed critical threat.
- "confidence": integer 0-100. How confident you are in the threat_score given the quality of available data.
- "severity": one of "Low", "Medium", "High", "Critical".
- "verdict": one of "Benign", "Suspicious", "Likely Phishing", "Confirmed Phishing", "Malware Distribution", "Credential Harvesting".
- "indicators": array of strings. Specific observed red flags. Be technical and precise. Examples: "Domain registered 3 days ago (isSuspiciousAge=true)", "VirusTotal: 14/90 engines flagged as phishing", "SSL certificate issued by Let's Encrypt to a typosquatted domain", "Redirect chain leads to known phishing kit infrastructure".
- "techniques": array of strings. High-level attack techniques. Examples: "typosquatting", "homograph attack (IDN)", "credential harvesting form", "open redirect abuse", "brand impersonation (PayPal)", "drive-by download".
- "mitre_techniques": array of objects with fields { "id": string, "name": string, "tactic": string }. Map detected behaviors to MITRE ATT&CK for Enterprise (T-codes). Include relevant sub-techniques. Example: { "id": "T1566.002", "name": "Spearphishing Link", "tactic": "Initial Access" }.
- "brand_impersonated": string or null. Which brand/org is being impersonated if any (e.g., "PayPal", "Microsoft 365", "HDFC Bank", null).
- "ai_summary": string. A concise but expert 2-3 sentence forensic assessment. Be direct and authoritative. Do NOT say "could be" — make definitive statements based on evidence. State the threat, how it operates, and the likely target.

If data is missing for a field (e.g., no WHOIS data), still assess what you can from the available data.`
          },
          {
            role: 'user',
            content: `Analyze this forensic evidence for a suspected phishing/malicious site:

TARGET: ${analysisData.url}
TARGET TYPE: ${analysisData.targetType || 'URL'}

WHOIS REGISTRY DATA:
${JSON.stringify(analysisData.whois, null, 2)}

IP GEOLOCATION:
${JSON.stringify(analysisData.ipGeo, null, 2)}

SSL CERTIFICATE INFO:
${JSON.stringify(analysisData.ssl, null, 2)}

VIRUSTOTAL SCAN RESULTS:
${JSON.stringify(analysisData.virustotal, null, 2)}

DOMAIN SIMILARITY CHECK (brand impersonation detection):
${JSON.stringify(analysisData.similarity, null, 2)}

HOMOGRAPH / IDN ATTACK DETECTION:
${JSON.stringify(analysisData.homograph, null, 2)}

REDIRECT CHAIN (full request path observed):
${JSON.stringify(analysisData.redirectChain, null, 2)}

Produce a complete forensic JSON assessment as specified.`
          }
        ]
      })
    });

    if (!response.ok) {
      const errBody = await response.text().catch(() => '');
      throw new Error(`OpenAI API responded with status ${response.status}: ${errBody.slice(0, 200)}`);
    }

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content;
    if (!content) {
      throw new Error('OpenAI returned an empty or null response content');
    }
    const parsed = JSON.parse(content);

    return {
      threat_score: Number(parsed.threat_score) || 0,
      confidence: Number(parsed.confidence) || 50,
      severity: parsed.severity || 'Low',
      verdict: parsed.verdict || 'Suspicious',
      brand_impersonated: parsed.brand_impersonated || null,
      indicators: Array.isArray(parsed.indicators) ? parsed.indicators : [],
      techniques: Array.isArray(parsed.techniques) ? parsed.techniques : [],
      mitre_techniques: Array.isArray(parsed.mitre_techniques) ? parsed.mitre_techniques : [],
      ai_summary: parsed.ai_summary || ''
    };
  } catch (err) {
    logger.error('OpenAI Analysis Error', { error: err.message, stack: err.stack, url: analysisData.url });
    return {
      threat_score: 50,
      confidence: 20,
      severity: 'Medium',
      verdict: 'Suspicious',
      brand_impersonated: null,
      indicators: [`AI analysis failed: ${err.message}`],
      techniques: [],
      mitre_techniques: [],
      ai_summary: 'An error occurred during AI analysis. Threat score defaults to 50. Manual forensic review is required.'
    };
  }
};
