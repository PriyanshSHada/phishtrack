'use strict';

const nock = require('nock');
const dns = require('dns').promises;

// Mock dns.resolve4 for all tests
jest.mock('dns', () => ({
  promises: {
    resolve4: jest.fn()
  }
}));

const ipgeoService = require('../../../src/services/analysis/ipgeo.service');

const SUCCESS_RESPONSE = {
  status: 'success',
  country: 'United States',
  countryCode: 'US',
  regionName: 'California',
  city: 'San Francisco',
  zip: '94105',
  lat: 37.7749,
  lon: -122.4194,
  isp: 'Cloudflare Inc.',
  org: 'Cloudflare',
  as: 'AS13335 Cloudflare, Inc.'
};

beforeEach(() => {
  jest.clearAllMocks();
  nock.cleanAll();
});

afterAll(() => {
  nock.restore();
});

describe('ipgeo.service', () => {
  test('U17 — returns geo object for valid URL when ip-api returns success', async () => {
    dns.resolve4.mockResolvedValue(['104.18.1.1']);
    nock('http://ip-api.com')
      .get('/json/104.18.1.1')
      .reply(200, SUCCESS_RESPONSE);

    const result = await ipgeoService.getIpGeoData('https://example.com/path');
    expect(result).not.toBeNull();
    expect(result.ip).toBe('104.18.1.1');
    expect(result.country).toBe('United States');
    expect(result.lat).toBe(37.7749);
    expect(result.lon).toBe(-122.4194);
  });

  test('U18 — returns null when ip-api reports status: fail (private IP)', async () => {
    dns.resolve4.mockResolvedValue(['192.168.1.1']);
    nock('http://ip-api.com')
      .get('/json/192.168.1.1')
      .reply(200, { status: 'fail', message: 'private range', query: '192.168.1.1' });

    const result = await ipgeoService.getIpGeoData('https://internal.local');
    expect(result).toBeNull();
  });

  test('U19 — returns null when DNS resolution fails', async () => {
    dns.resolve4.mockRejectedValue(new Error('ENOTFOUND unknownhost.invalid'));
    const result = await ipgeoService.getIpGeoData('https://unknownhost.invalid');
    expect(result).toBeNull();
  });

  test('U20 — returns null when ip-api returns HTTP error', async () => {
    dns.resolve4.mockResolvedValue(['1.2.3.4']);
    nock('http://ip-api.com')
      .get('/json/1.2.3.4')
      .reply(503, 'Service Unavailable');

    const result = await ipgeoService.getIpGeoData('https://example.com');
    expect(result).toBeNull();
  });

  test('U21 — returns null for URL where DNS returns empty address list', async () => {
    dns.resolve4.mockResolvedValue([]);
    // No ip-api call should be made since no IP was resolved
    const result = await ipgeoService.getIpGeoData('https://noresult.example');
    expect(result).toBeNull();
  });

  test('U22 — handles lat: 0, lon: 0 (equator) correctly — not treated as missing', async () => {
    dns.resolve4.mockResolvedValue(['102.0.0.1']);
    nock('http://ip-api.com')
      .get('/json/102.0.0.1')
      .reply(200, { ...SUCCESS_RESPONSE, lat: 0, lon: 0 });

    const result = await ipgeoService.getIpGeoData('https://equator.example');
    expect(result).not.toBeNull();
    expect(result.lat).toBe(0);
    expect(result.lon).toBe(0);
  });
});
