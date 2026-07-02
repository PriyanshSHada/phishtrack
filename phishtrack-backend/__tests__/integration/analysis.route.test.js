'use strict';

const request = require('supertest');
const nock = require('nock');
const app = require('../../src/app');
const prisma = require('../../src/prismaClient');
const { resetDatabase, makeUser } = require('./helpers');

jest.mock('../../src/redisClient', () => ({
  isOpen: false, on: jest.fn(), connect: jest.fn()
}));

// Mock all external HTTP calls used by the analysis pipeline
function setupAnalysisMocks() {
  nock.cleanAll();
  // ip-api (geolocation)
  nock('http://ip-api.com').get(/\/json\/.*/).reply(200, {
    status: 'success',
    country: 'United States',
    lat: 37.7,
    lon: -122.4,
    isp: 'Test ISP',
    city: 'San Francisco',
    countryCode: 'US',
    regionName: 'California'
  }).persist();

  // VirusTotal — return "URL not found" so it submits a scan
  nock('https://www.virustotal.com')
    .get(/\/api\/v3\/urls\/.*/).reply(404, {})
    .post('/api/v3/urls').reply(200, { data: { id: 'scan-123' } })
    .persist();

  // Fireworks AI
  nock('https://api.fireworks.ai').post('/inference/v1/chat/completions').reply(200, {
    choices: [{
      message: {
        content: JSON.stringify({
          threat_score: 72,
          severity: 'High',
          indicators: ['Suspicious domain age'],
          techniques: ['phishing'],
          ai_summary: 'Test AI summary'
        })
      }
    }]
  }).persist();

  // Brevo (no emails in tests)
  nock('https://api.brevo.com').post('/v3/smtp/email').reply(201, {}).persist();
}

let authToken, authUser, caseId;

beforeAll(async () => {
  await resetDatabase();
  setupAnalysisMocks();

  process.env.FIREWORKS_API_KEY = 'sk-test-key';
  process.env.FIREWORKS_API_BASE = 'https://api.fireworks.ai/inference/v1';
  process.env.FIREWORKS_MODEL = 'accounts/fireworks/models/glm-5p2';
  process.env.VIRUSTOTAL_API_KEY = 'vt-test-key';

  const result = await makeUser({ email: 'analysis-test@example.com' });
  authToken = result.token;
  authUser = result.user;

  // Create a case to run analysis on
  const c = await prisma.case.create({
    data: {
      case_number: 'CASE-2026-AN01',
      userId: authUser.id,
      url: 'https://phish.testsite.example.com',
      source: 'Email',
      priority: 'High',
      tags: []
    }
  });
  caseId = c.id;
});

afterAll(async () => {
  nock.cleanAll();
  delete process.env.FIREWORKS_API_KEY;
  delete process.env.FIREWORKS_API_BASE;
  delete process.env.FIREWORKS_MODEL;
  delete process.env.VIRUSTOTAL_API_KEY;
  await prisma.$disconnect();
});

const authHeader = () => ({ Authorization: `Bearer ${authToken}` });

describe('Analysis Routes — Integration', () => {
  describe('POST /api/analysis/run', () => {
    test('I33 — valid caseId triggers analysis and returns 201', async () => {
      const res = await request(app)
        .post('/api/analysis/run')
        .set(authHeader())
        .send({ caseId });

      expect(res.statusCode).toBe(201);
      expect(res.body.caseId).toBe(caseId);
      expect(typeof res.body.threat_score).toBe('number');
    }, 30000);

    test('I34 — missing caseId returns 400', async () => {
      const res = await request(app)
        .post('/api/analysis/run')
        .set(authHeader())
        .send({});
      expect(res.statusCode).toBe(400);
    });

    test('I35 — non-existent case returns 404', async () => {
      const res = await request(app)
        .post('/api/analysis/run')
        .set(authHeader())
        .send({ caseId: '00000000-0000-0000-0000-000000000099' });
      expect(res.statusCode).toBe(404);
    });

    test('I36 — all sub-services fail still returns 201 with default threat score', async () => {
      // Create a fresh case
      const c2 = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-AN02',
          userId: authUser.id,
          url: 'https://failing.example.com',
          source: 'SMS',
          priority: 'Low',
          tags: []
        }
      });

      // Override mocks to fail
      nock.cleanAll();
      nock('http://ip-api.com').get(/\/json\/.*/).reply(500, {}).persist();
      nock('https://www.virustotal.com').get(/\/api\/v3\/urls\/.*/).reply(500, {}).persist();
      nock('https://api.fireworks.ai').post('/inference/v1/chat/completions').reply(500, {}).persist();

      const res = await request(app)
        .post('/api/analysis/run')
        .set(authHeader())
        .send({ caseId: c2.id });

      expect(res.statusCode).toBe(201);
      expect(res.body.threat_score).toBe(50); // Default fallback
    }, 30000);
  });

  describe('GET /api/analysis/:caseId', () => {
    test('I37 — existing analysis returns 200', async () => {
      const res = await request(app)
        .get(`/api/analysis/${caseId}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.caseId).toBe(caseId);
    });

    test('I38 — caseId with no analysis returns 404', async () => {
      const emptyCase = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-NOANA',
          userId: authUser.id,
          url: 'https://noanalysis.example.com',
          source: 'Other',
          priority: 'Low',
          tags: []
        }
      });

      const res = await request(app)
        .get(`/api/analysis/${emptyCase.id}`)
        .set(authHeader());
      expect(res.statusCode).toBe(404);
    });
  });
});
