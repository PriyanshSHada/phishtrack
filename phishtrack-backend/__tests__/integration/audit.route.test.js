'use strict';

const request = require('supertest');
const app = require('../../src/app');
const prisma = require('../../src/prismaClient');
const { resetDatabase, makeUser } = require('./helpers');

jest.mock('../../src/redisClient', () => ({
  isOpen: false, on: jest.fn(), connect: jest.fn()
}));

let authToken, authUser;

beforeAll(async () => {
  await resetDatabase();
  const result = await makeUser({ email: 'audit-test@example.com' });
  authToken = result.token;
  authUser = result.user;

  // Seed audit logs
  for (let i = 0; i < 5; i++) {
    await prisma.auditLog.create({
      data: {
        userId: authUser.id,
        action: i === 0 ? 'USER_LOGIN_SUCCESS' : 'CASE_CREATED',
        ip_address: '127.0.0.1',
        timestamp: new Date()
      }
    });
  }
});

afterAll(async () => { await prisma.$disconnect(); });

const authHeader = () => ({ Authorization: `Bearer ${authToken}` });

describe('Audit Routes — Integration', () => {
  test('I55 — GET /api/audit/logs returns array of audit entries', async () => {
    const res = await request(app)
      .get('/api/audit/logs')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThanOrEqual(5);
  });

  test('I57 — audit log created automatically after POST /api/cases', async () => {
    const before = await prisma.auditLog.count({ where: { action: 'CASE_CREATED' } });

    await request(app)
      .post('/api/cases')
      .set(authHeader())
      .send({ url: 'https://audit-trigger.example.com', priority: 'Low', source: 'Other' });

    // Short wait for async audit write
    await new Promise(r => setTimeout(r, 200));

    const after = await prisma.auditLog.count({ where: { action: 'CASE_CREATED' } });
    expect(after).toBeGreaterThan(before);
  });

  describe('GET /api/audit/custody/:caseId', () => {
    test('I56 — returns 200 with chain of custody records', async () => {
      // Create a case and generate a report to trigger custody chain entries
      const c = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-AUD',
          userId: authUser.id,
          url: 'https://custody.example.com',
          source: 'Email',
          priority: 'Medium',
          tags: []
        }
      });

      // Manually add a custody record
      await prisma.chainOfCustody.create({
        data: {
          caseId: c.id,
          userId: authUser.id,
          action: 'CASE_CREATED',
          hash_before: null,
          hash_after: 'abc123def456'
        }
      });

      const res = await request(app)
        .get(`/api/audit/custody/${c.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(Array.isArray(res.body)).toBe(true);
      expect(res.body.length).toBeGreaterThanOrEqual(1);
    });
  });
});
