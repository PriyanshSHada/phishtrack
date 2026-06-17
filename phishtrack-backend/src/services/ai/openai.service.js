const logger = require('../../utils/logger');

exports.analyzeWithAi = async (analysisData) => {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    logger.warn('OpenAI API key not configured');
    return {
      threat_score: 50,
      severity: 'Medium',
      indicators: ['OpenAI API Key not configured'],
      techniques: [],
      ai_summary: 'AI analysis could not run because the OpenAI API key is missing. Threat score defaults to 50.'
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
            content: `You are an expert forensic phishing analyst. You will analyze the forensic data provided and return a structured JSON report.
Your output JSON MUST contain exactly these fields:
- "threat_score": integer between 0 and 100
- "severity": string (must be one of: "Low", "Medium", "High", "Critical")
- "indicators": array of strings (list of specific suspicious signs, e.g., "Domain age < 30 days", "SSL not secure", "VirusTotal flags")
- "techniques": array of strings (techniques used, e.g., "typosquatting", "form hijacking", "social engineering")
- "ai_summary": string (a concise 1-paragraph summary explaining the analysis and threat assessment)`
          },
          {
            role: 'user',
            content: `Analyze the following website data:
URL: ${analysisData.url}
WHOIS Info: ${JSON.stringify(analysisData.whois)}
IP Geolocation: ${JSON.stringify(analysisData.ipGeo)}
SSL Info: ${JSON.stringify(analysisData.ssl)}
VirusTotal Result: ${JSON.stringify(analysisData.virustotal)}
Similar Domain Detection: ${JSON.stringify(analysisData.similarity)}
Redirect Chain: ${JSON.stringify(analysisData.redirectChain)}`
          }
        ]
      })
    });

    if (!response.ok) {
      throw new Error(`OpenAI API responded with status ${response.status}`);
    }

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content;
    if (!content) {
      throw new Error('OpenAI returned an empty or null response content');
    }
    const parsed = JSON.parse(content);

    return {
      threat_score: Number(parsed.threat_score) || 0,
      severity: parsed.severity || 'Low',
      indicators: Array.isArray(parsed.indicators) ? parsed.indicators : [],
      techniques: Array.isArray(parsed.techniques) ? parsed.techniques : [],
      ai_summary: parsed.ai_summary || ''
    };
  } catch (err) {
    logger.error('OpenAI Analysis Error', { error: err.message, stack: err.stack, data: analysisData });
    return {
      threat_score: 50,
      severity: 'Medium',
      indicators: [`AI analysis failed: ${err.message}`],
      techniques: [],
      ai_summary: 'An error occurred during AI analysis. Threat score defaults to 50.'
    };
  }
};
