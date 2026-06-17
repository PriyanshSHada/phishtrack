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
  const result = await makeUser({ email: 'cases-test@example.com' });
  authToken = result.token;
  authUser = result.user;
});

afterAll(async () => { await prisma.$disconnect(); });

const authHeader = () => ({ Authorization: `Bearer ${authToken}` });

describe('Cases Routes — Integration', () => {
  // ── Create ──────────────────────────────────────────────────────────────────
  describe('POST /api/cases', () => {
    test('I16 — valid body returns 201 with generated case_number', async () => {
      const res = await request(app)
        .post('/api/cases')
        .set(authHeader())
        .send({ url: 'https://phish.example.com', priority: 'High', source: 'Email' });

      expect(res.statusCode).toBe(201);
      expect(res.body.case_number).toMatch(/^CASE-\d{4}-\d{3,}/);
      expect(res.body.url).toBe('https://phish.example.com');
    });

    test('I17 — missing URL returns 400', async () => {
      const res = await request(app)
        .post('/api/cases')
        .set(authHeader())
        .send({ priority: 'High', source: 'Email' });

      expect(res.statusCode).toBe(400);
    });

    test('I18 — invalid priority value returns 400', async () => {
      const res = await request(app)
        .post('/api/cases')
        .set(authHeader())
        .send({ url: 'https://example.com', priority: 'Extreme', source: 'Email' });

      expect(res.statusCode).toBe(400);
    });

    test('I19 — tags array of 11 items (over limit) returns 400', async () => {
      const res = await request(app)
        .post('/api/cases')
        .set(authHeader())
        .send({
          url: 'https://example.com',
          priority: 'Low',
          source: 'Other',
          tags: Array(11).fill('tag')
        });

      expect(res.statusCode).toBe(400);
    });

    test('I32 — request without token returns 401', async () => {
      const res = await request(app)
        .post('/api/cases')
        .send({ url: 'https://example.com' });
      expect(res.statusCode).toBe(401);
    });
  });

  // ── List ────────────────────────────────────────────────────────────────────
  describe('GET /api/cases', () => {
    beforeAll(async () => {
      // Create 5 cases for filtering/pagination tests
      for (let i = 0; i < 5; i++) {
        await prisma.case.create({
          data: {
            case_number: `CASE-2026-TC${10 + i}`,
            userId: authUser.id,
            url: `https://phish${i}.example.com`,
            source: 'Email',
            priority: i % 2 === 0 ? 'Critical' : 'Low',
            status: i % 3 === 0 ? 'Open' : 'Investigating',
            tags: []
          }
        });
      }
    });

    test('I20 — no filters returns 200 with data array and pagination', async () => {
      const res = await request(app)
        .get('/api/cases')
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(Array.isArray(res.body.data)).toBe(true);
      expect(res.body.pagination).toBeDefined();
    });

    test('I21 — status=Open filter returns only Open/Investigating cases', async () => {
      const res = await request(app)
        .get('/api/cases?status=Open')
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      res.body.data.forEach(c => {
        expect(['Open', 'Investigating']).toContain(c.status);
      });
    });

    test('I22 — priority=Critical filter returns only Critical cases', async () => {
      const res = await request(app)
        .get('/api/cases?priority=Critical')
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      res.body.data.forEach(c => {
        expect(c.priority).toBe('Critical');
      });
    });

    test('I23 — pagination: page=1&limit=2 returns ≤2 results with pages > 1', async () => {
      const res = await request(app)
        .get('/api/cases?page=1&limit=2')
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.data.length).toBeLessThanOrEqual(2);
      expect(res.body.pagination.pages).toBeGreaterThan(1);
    });
  });

  // ── Get By Id ───────────────────────────────────────────────────────────────
  describe('GET /api/cases/:id', () => {
    let caseId;

    beforeAll(async () => {
      const c = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-SINGLE',
          userId: authUser.id,
          url: 'https://single.example.com',
          source: 'SMS',
          priority: 'Medium',
          tags: []
        }
      });
      caseId = c.id;
    });

    test('I24 — existing case returns 200 with analyses and reports', async () => {
      const res = await request(app)
        .get(`/api/cases/${caseId}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(res.body.id).toBe(caseId);
      expect(Array.isArray(res.body.analyses)).toBe(true);
      expect(Array.isArray(res.body.reports)).toBe(true);
    });

    test('I25 — non-existent case id returns 404', async () => {
      const res = await request(app)
        .get('/api/cases/00000000-0000-0000-0000-000000000000')
        .set(authHeader());
      expect(res.statusCode).toBe(404);
    });
  });

  // ── Update ──────────────────────────────────────────────────────────────────
  describe('PUT /api/cases/:id', () => {
    let updateCaseId;

    beforeAll(async () => {
      const c = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-UPD',
          userId: authUser.id,
          url: 'https://update.example.com',
          source: 'WhatsApp',
          priority: 'Low',
          tags: []
        }
      });
      updateCaseId = c.id;
    });

    test('I26 — change status to Closed returns 200 with updated status', async () => {
      const res = await request(app)
        .put(`/api/cases/${updateCaseId}`)
        .set(authHeader())
        .send({ status: 'Closed' });

      expect(res.statusCode).toBe(200);
      expect(res.body.status).toBe('Closed');
    });

    test('I27 — invalid status value returns 400', async () => {
      const res = await request(app)
        .put(`/api/cases/${updateCaseId}`)
        .set(authHeader())
        .send({ status: 'Archived' });
      expect(res.statusCode).toBe(400);
    });

    test('I28 — empty body returns 400', async () => {
      const res = await request(app)
        .put(`/api/cases/${updateCaseId}`)
        .set(authHeader())
        .send({});
      expect(res.statusCode).toBe(400);
    });
  });

  // ── Delete ──────────────────────────────────────────────────────────────────
  describe('DELETE /api/cases/:id', () => {
    test('I29 — deletes existing case and returns 200', async () => {
      const c = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-DEL',
          userId: authUser.id,
          url: 'https://delete.example.com',
          source: 'Other',
          priority: 'Low',
          tags: []
        }
      });

      const res = await request(app)
        .delete(`/api/cases/${c.id}`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      const check = await prisma.case.findUnique({ where: { id: c.id } });
      expect(check).toBeNull();
    });

    test('I30 — delete non-existent case returns 404', async () => {
      const res = await request(app)
        .delete('/api/cases/00000000-0000-0000-0000-000000000001')
        .set(authHeader());
      expect(res.statusCode).toBe(404);
    });
  });

  // ── Timeline ────────────────────────────────────────────────────────────────
  describe('GET /api/cases/:id/timeline', () => {
    test('I31 — returns sorted timeline array', async () => {
      const c = await prisma.case.create({
        data: {
          case_number: 'CASE-2026-TL',
          userId: authUser.id,
          url: 'https://timeline.example.com',
          source: 'Email',
          priority: 'Low',
          tags: []
        }
      });

      const res = await request(app)
        .get(`/api/cases/${c.id}/timeline`)
        .set(authHeader());

      expect(res.statusCode).toBe(200);
      expect(Array.isArray(res.body)).toBe(true);
    });
  });
});
