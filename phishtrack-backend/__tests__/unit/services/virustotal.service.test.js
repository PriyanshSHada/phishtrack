'use strict';

const nock = require('nock');

const vtService = require('../../../src/services/analysis/virustotal.service');

const VT_API = 'https://www.virustotal.com';
const FAKE_KEY = 'test-vt-api-key-1234';

const SCAN_RESULT = {
  data: {
    attributes: {
      last_analysis_stats: { malicious: 3, suspicious: 1, harmless: 60, undetected: 10 },
      last_analysis_results: {
        'EngineA': { category: 'malicious', result: 'Phishing.Site' },
        'EngineB': { category: 'suspicious', result: 'Suspicious.URL' },
        'EngineC': { category: 'harmless', result: null }
      }
    }
  }
};

/**
 * Replicate exactly the same URL normalization the service does so we can
 * compute the correct nock path for each test URL.
 */
function serviceUrlId(rawUrl) {
  let normalizedUrl = rawUrl;
  try {
    const parsed = new URL(rawUrl);
    parsed.protocol = parsed.protocol.toLowerCase();
    parsed.hostname = parsed.hostname.toLowerCase();
    normalizedUrl = parsed.toString();
  } catch (_) { /* fall back */ }
  return Buffer.from(normalizedUrl).toString('base64')
    .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

beforeEach(() => {
  nock.cleanAll();
  delete process.env.VIRUSTOTAL_API_KEY;
});

afterAll(() => nock.cleanAll());

describe('virustotal.service', () => {
  test('U35 — no API key returns { error: "VirusTotal API key not configured" }', async () => {
    const result = await vtService.getVirusTotalResult('https://example.com');
    expect(result.error).toMatch(/not configured/i);
  });

  test('U36 — 200 response returns scanned: true with correct counts and detections', async () => {
    process.env.VIRUSTOTAL_API_KEY = FAKE_KEY;
    const url = 'https://example.com';
    nock(VT_API).get(`/api/v3/urls/${serviceUrlId(url)}`).reply(200, SCAN_RESULT);

    const result = await vtService.getVirusTotalResult(url);
    expect(result.scanned).toBe(true);
    expect(result.maliciousCount).toBe(3);
    expect(result.suspiciousCount).toBe(1);
    expect(result.detections).toHaveLength(2);
    expect(result.detections[0].engine).toBe('EngineA');
  });

  test('U37 — 404 from VT triggers URL submission and returns scanned: false', async () => {
    process.env.VIRUSTOTAL_API_KEY = FAKE_KEY;
    const url = 'https://new-site.example';
    nock(VT_API).get(`/api/v3/urls/${serviceUrlId(url)}`).reply(404, {});
    nock(VT_API).post('/api/v3/urls').reply(200, { data: { id: 'scan-123' } });

    const result = await vtService.getVirusTotalResult(url);
    expect(result.scanned).toBe(false);
    expect(result.scanId).toBe('scan-123');
    expect(result.message).toMatch(/submitted for scanning/i);
  });

  test('U38 — 429 rate limit from VT returns error object', async () => {
    process.env.VIRUSTOTAL_API_KEY = FAKE_KEY;
    const url = 'https://ratelimited.com';
    nock(VT_API).get(`/api/v3/urls/${serviceUrlId(url)}`).reply(429, { error: 'QuotaExceededError' });

    const result = await vtService.getVirusTotalResult(url);
    expect(result.error).toBeDefined();
  });

  test('U39 — URL with uppercase scheme/host normalizes to same ID as lowercase', async () => {
    process.env.VIRUSTOTAL_API_KEY = FAKE_KEY;
    const upperUrl = 'HTTPS://EXAMPLE.COM/';
    const lowerUrl = 'https://example.com/';
    // Both should produce the same ID after normalization
    expect(serviceUrlId(upperUrl)).toBe(serviceUrlId(lowerUrl));

    nock(VT_API).get(`/api/v3/urls/${serviceUrlId(upperUrl)}`).reply(200, SCAN_RESULT);

    const result = await vtService.getVirusTotalResult(upperUrl);
    expect(result.scanned).toBe(true);
  });

  test('U40 — URL with special characters does not throw', async () => {
    process.env.VIRUSTOTAL_API_KEY = FAKE_KEY;
    const url = 'https://example.com/path?q=héllo&r=wörld';
    nock(VT_API).get(/\/api\/v3\/urls\/.*/).reply(404, {});
    nock(VT_API).post('/api/v3/urls').reply(200, { data: { id: 'scan-456' } });

    await expect(vtService.getVirusTotalResult(url)).resolves.not.toThrow();
  });
});
