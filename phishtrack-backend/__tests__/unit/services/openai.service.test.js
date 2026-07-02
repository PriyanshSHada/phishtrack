'use strict';

const nock = require('nock');
const openaiService = require('../../../src/services/ai/openai.service');

const FIREWORKS_API = 'https://api.fireworks.ai';
const FAKE_KEY = 'sk-test-1234567890';

const SAMPLE_DATA = {
  url: 'https://phish.example.com',
  whois: { ageDays: 5, isSuspiciousAge: true },
  ipGeo: { country: 'Russia', lat: 55.7, lon: 37.6 },
  ssl: { valid: false, error: 'Protocol is not HTTPS' },
  virustotal: { scanned: true, maliciousCount: 5 },
  similarity: { isBrandDomain: false, similarTo: 'paypal.com', reason: 'contains_brand_name' },
  redirectChain: ['http://phish.example.com', 'http://evil.com']
};

const makeOpenAiResponse = (content) => ({
  choices: [{ message: { content: JSON.stringify(content) } }]
});

beforeEach(() => {
  nock.cleanAll();
  delete process.env.FIREWORKS_API_KEY;
  delete process.env.FIREWORKS_API_BASE;
  delete process.env.FIREWORKS_MODEL;
});

afterAll(() => nock.cleanAll());

describe('openai.service', () => {
  test('U41 — no API key returns default { threat_score: 50, severity: Medium }', async () => {
    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(result.threat_score).toBe(50);
    expect(result.severity).toBe('Medium');
    expect(result.indicators[0]).toMatch(/API Key not configured/i);
  });

  test('U42 — successful GPT response parses all fields correctly', async () => {
    process.env.FIREWORKS_API_KEY = FAKE_KEY;
    const gptContent = {
      threat_score: 88,
      severity: 'Critical',
      indicators: ['Domain age < 30 days', 'VT malicious count: 5'],
      techniques: ['typosquatting', 'redirect-chain'],
      ai_summary: 'This is a high-confidence phishing site.'
    };
    nock(FIREWORKS_API).post('/inference/v1/chat/completions').reply(200, makeOpenAiResponse(gptContent));

    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(result.threat_score).toBe(88);
    expect(result.severity).toBe('Critical');
    expect(result.indicators).toEqual(gptContent.indicators);
    expect(result.techniques).toEqual(gptContent.techniques);
    expect(result.ai_summary).toBe(gptContent.ai_summary);
  });

  test('U43 — GPT returns non-JSON content falls back to defaults', async () => {
    process.env.FIREWORKS_API_KEY = FAKE_KEY;
    nock(FIREWORKS_API).post('/inference/v1/chat/completions').reply(200, {
      choices: [{ message: { content: 'I cannot provide a JSON response here.' } }]
    });

    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(result.threat_score).toBe(50);
    expect(result.severity).toBe('Medium');
  });

  test('U44 — Fireworks HTTP 500 falls back to default with error in indicators', async () => {
    process.env.FIREWORKS_API_KEY = FAKE_KEY;
    nock(FIREWORKS_API).post('/inference/v1/chat/completions').reply(500, { error: 'Server Error' });

    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(result.threat_score).toBe(50);
    expect(result.indicators.some(i => /AI analysis failed/i.test(i))).toBe(true);
  });

  test('U45 — threat_score returned as string "75" is coerced to number 75', async () => {
    process.env.FIREWORKS_API_KEY = FAKE_KEY;
    const gptContent = {
      threat_score: '75',
      severity: 'High',
      indicators: [],
      techniques: [],
      ai_summary: 'Moderate threat.'
    };
    nock(FIREWORKS_API).post('/inference/v1/chat/completions').reply(200, makeOpenAiResponse(gptContent));

    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(result.threat_score).toBe(75);
    expect(typeof result.threat_score).toBe('number');
  });

  test('U45b — missing indicators/techniques fields default to empty arrays', async () => {
    process.env.FIREWORKS_API_KEY = FAKE_KEY;
    const gptContent = { threat_score: 60, severity: 'Medium', ai_summary: 'ok' };
    nock(FIREWORKS_API).post('/inference/v1/chat/completions').reply(200, makeOpenAiResponse(gptContent));

    const result = await openaiService.analyzeWithAi(SAMPLE_DATA);
    expect(Array.isArray(result.indicators)).toBe(true);
    expect(Array.isArray(result.techniques)).toBe(true);
  });
});
