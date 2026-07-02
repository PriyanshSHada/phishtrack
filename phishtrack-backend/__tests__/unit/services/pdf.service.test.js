'use strict';

const pdfService = require('../../../src/services/report/pdf.service');

const buildBaseData = (analysisOverrides = {}) => ({
  case: {
    case_number: 'CASE-UNIT-001',
    url: 'https://example.com',
    target_type: 'URL',
    priority: 'High',
    source: 'Email',
    status: 'Open',
    created_at: new Date().toISOString()
  },
  analysis: {
    threat_score: 77,
    confidence: 88,
    severity: 'High',
    verdict: 'Likely Phishing',
    brand_impersonated: 'Example',
    whois_data: {
      registrar: 'Example Registrar',
      country: 'US',
      creationDate: new Date().toISOString(),
      expiryDate: new Date().toISOString(),
      ageDays: 2,
      isSuspiciousAge: true
    },
    ip_geolocation: {
      country: 'US',
      city: 'Austin',
      isp: 'Example ISP',
      ip: '203.0.113.4'
    },
    ssl_info: {
      valid: false,
      issuer: 'Example CA',
      validTo: new Date().toISOString()
    },
    virustotal_result: {
      maliciousCount: 3,
      suspiciousCount: 1,
      harmlessCount: 60,
      totalEngines: 64,
      detections: [{ engine: 'AV1', category: 'malicious', result: 'phishing' }]
    },
    ai_indicators: ['Indicator A'],
    ai_techniques: ['Technique A'],
    redirect_chain: ['https://example.com', 'https://login.example.com'],
    page_screenshot: null,
    ai_summary: 'Summary',
    mitre_techniques: [{ id: 'T1566.002', name: 'Spearphishing Link', tactic: 'Initial Access' }],
    ...analysisOverrides
  },
  analyst: {
    name: 'Tester',
    email: 'tester@example.com'
  },
  digitalSignature: 'abc',
  version: 1,
  generated_at: new Date().toISOString(),
  custodyChain: []
});

describe('pdf.service', () => {
  test('renders a report when WHOIS and screenshot are missing', async () => {
    const buffer = await pdfService.generatePdfReport(buildBaseData({
      whois_data: null,
      page_screenshot: null
    }));

    expect(Buffer.isBuffer(buffer)).toBe(true);
    expect(buffer.length).toBeGreaterThan(1000);
  });

  test('renders a report using legacy field aliases', async () => {
    const buffer = await pdfService.generatePdfReport(buildBaseData({
      whois_data: undefined,
      ip_geolocation: undefined,
      ssl_info: undefined,
      virustotal_result: undefined,
      redirect_chain: undefined,
      page_screenshot: null,
      ai_indicators: undefined,
      ai_techniques: undefined,
      whois: {
        registrar: 'Legacy Registrar',
        country: 'GB',
        ageDays: 9,
        isSuspiciousAge: true
      },
      ipGeo: {
        country: 'GB',
        city: 'London',
        isp: 'Legacy ISP',
        ip: '198.51.100.10'
      },
      ssl: {
        valid: true,
        issuer: 'Legacy CA'
      },
      virustotal: {
        maliciousCount: 2,
        suspiciousCount: 1,
        harmlessCount: 61,
        totalEngines: 64,
        detections: []
      },
      redirectChain: ['https://example.com', 'https://login.example.com'],
      indicators: ['Legacy indicator'],
      techniques: ['Legacy technique'],
      screenshot: null
    }));

    expect(Buffer.isBuffer(buffer)).toBe(true);
    expect(buffer.length).toBeGreaterThan(1000);
  });
});