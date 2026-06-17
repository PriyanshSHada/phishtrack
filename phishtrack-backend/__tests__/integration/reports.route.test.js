'use strict';

const request = require('supertest');
const app = require('../../src/app');
const prisma = require('../../src/prismaClient');
const { resetDatabase, makeUser } = require('./helpers');

jest.mock('../../src/redisClient', () => ({
  isOpen: false, on: jest.fn(), connect: jest.fn()
}));

let authToken, authUser;
let testCase, testAnalysis;

beforeAll(async () => {
  await resetDatabase();
  const result = await makeUser({ email: 'reports-test@example.com' });
  authToken = result.token;
  authUser = result.user;

  // Create case
  testCase = await prisma.case.create({
    data: {
      case_number: 'CASE-2026-RPT01',
      userId: authUser.id,
      url: 'https://phishtest.example.com',
      source: 'Email',
      priority: 'High',
      tags: []
    }
  });

  // Create analysis
  testAnalysis = await prisma.analysis.create({
    data: {
      caseId: testCase.id,
      threat_score: 85,
      severity: 'High',
      ai_summary: 'Test phishing site detected.',
      ai_indicators: ['Short domain age'],
      ai_techniques: ['URL obfuscation'],
      redirect_chain: []
    }
  });
});

afterAll(async () => { await prisma.$disconnect(); });

const authHeader = () => ({ Authorization: `Bearer ${authToken}` });

describe('Reports Routes — Integration', () => {
  let reportId;

  describe('POST /api/reports/generate/:caseId', () => {
    test('I39 — valid case with analysis returns 201 with PDF and version 1', async () => {
      const res = await request(app)
        .post(`/api/reports/generate/${testCase.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(201);
      expect(res.body.version).toBe(1);
      expect(res.body.caseId).toBe(testCase.id);
      expect(res.body.digital_signature).toBeDefined();
      reportId = res.body.id;
    }, 20000);

    test('I42 — second report on same case returns version 2', async () => {
      const res = await request(app)
        .post(`/api/reports/generate/${testCase.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(201);
      expect(res.body.version).toBe(2);
    }, 20000);

    test('I40 — case with no analysis returns 400', async () => {
      const emptyCase = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-NORPT',
          userId: authUser.id,
          url: 'https://noreport.example.com',
          source: 'Other',
          priority: 'Low',
          tags: []
        }
      });

      const res = await request(app)
        .post(`/api/reports/generate/${emptyCase.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(400);
    });

    test('I41 — non-existent case returns 404', async () => {
      const res = await request(app)
        .post('/api/reports/generate/00000000-0000-0000-0000-000000000099')
        .set(authHeader());

      expect(res.statusCode).toBe(404);
    });
  });

  describe('GET /api/reports/:id', () => {
    test('I43 — existing report returns 200 with case and user', async () => {
      expect(reportId).toBeDefined();
      const res = await request(app)
        .get(`/api/reports/${reportId}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.id).toBe(reportId);
      expect(res.body.case).toBeDefined();
    });
  });

  describe('GET /api/reports/case/:caseId', () => {
    test('I44 — returns array of reports sorted by version desc', async () => {
      const res = await request(app)
        .get(`/api/reports/case/${testCase.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(Array.isArray(res.body)).toBe(true);
      expect(res.body.length).toBeGreaterThanOrEqual(2);
      // Most recent version first
      expect(res.body[0].version).toBeGreaterThan(res.body[1].version);
    });
  });

  describe('GET /api/reports/:id/verify', () => {
    test('I47 — freshly generated report is valid (untampered)', async () => {
      expect(reportId).toBeDefined();
      const res = await request(app)
        .get(`/api/reports/${reportId}/verify`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.valid).toBe(true);
    });

    test('I48 — tampered report returns valid: false', async () => {
      // Manually corrupt the signature
      await prisma.report.update({
        where: { id: reportId },
        data: { digital_signature: 'a'.repeat(64) }
      });

      const res = await request(app)
        .get(`/api/reports/${reportId}/verify`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.valid).toBe(false);
    });
  });

  describe('GET /api/reports/:id/pdf', () => {
    test('I45 — returns 200 with Content-Type application/pdf when file exists', async () => {
      // Generate a fresh report with a valid signature
      const rptRes = await request(app)
        .post(`/api/reports/generate/${testCase.id}`)
        .set(authHeader());

      const freshId = rptRes.body.id;

      const res = await request(app)
        .get(`/api/reports/${freshId}/pdf`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.headers['content-type']).toMatch(/application\/pdf/);
    }, 20000);
  });
});
