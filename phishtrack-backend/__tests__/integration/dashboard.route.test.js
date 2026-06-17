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
  const result = await makeUser({ email: 'dashboard-test@example.com' });
  authToken = result.token;
  authUser = result.user;

  // Create a second user
  const result2 = await makeUser({ email: 'dashboard-user2@example.com' });

  // Create 3 cases: 2 this week, 1 last week
  const now = new Date();
  const lastWeek = new Date(now.getTime() - 10 * 24 * 60 * 60 * 1000); // -10 days

  for (let i = 0; i < 2; i++) {
    await prisma.case.create({
      data: {
        case_number: `CASE-2026-DASH${i}`,
        userId: authUser.id,
        url: `https://dash${i}.example.com`,
        source: 'Email',
        priority: 'High',
        tags: [],
        created_at: now
      }
    });
  }

  // Last week case
  await prisma.case.create({
    data: {
      case_number: 'CASE-2026-DASHLW',
      userId: authUser.id,
      url: 'https://dashlastweek.example.com',
      source: 'SMS',
      priority: 'Low',
      tags: [],
      created_at: lastWeek
    }
  });
});

afterAll(async () => { await prisma.$disconnect(); });

const authHeader = () => ({ Authorization: `Bearer ${authToken}` });

describe('Dashboard Routes — Integration', () => {
  test('I50 — GET /api/dashboard/stats returns correct totals', async () => {
    const res = await request(app)
      .get('/api/dashboard/stats')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    expect(typeof res.body.users).toBe('number');
    expect(typeof res.body.cases).toBe('number');
    expect(res.body.users).toBeGreaterThanOrEqual(2);
    expect(res.body.cases).toBeGreaterThanOrEqual(3);
  });

  test('I51 — GET /api/dashboard/recent returns array of ≤10 cases newest first', async () => {
    const res = await request(app)
      .get('/api/dashboard/recent')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeLessThanOrEqual(10);

    // Newest case should be first
    if (res.body.length >= 2) {
      const d1 = new Date(res.body[0].createdAt);
      const d2 = new Date(res.body[1].createdAt);
      expect(d1.getTime()).toBeGreaterThanOrEqual(d2.getTime());
    }
  });

  test('I52 — GET /api/dashboard/weekly returns correct week totals and 28 days', async () => {
    const res = await request(app)
      .get('/api/dashboard/weekly')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    expect(res.body.totalThisWeek).toBe(2);
    expect(res.body.totalLastWeek).toBe(1);
    expect(Array.isArray(res.body.currentWeek)).toBe(true);
    expect(res.body.currentWeek).toHaveLength(28);
  });

  test('I53 — GET /api/dashboard/threat-map returns locations with lat/lon', async () => {
    // Create analysis with geo data
    const c = await prisma.case.findFirst({ where: { userId: authUser.id } });
    await prisma.analysis.create({
      data: {
        caseId: c.id,
        threat_score: 70,
        severity: 'High',
        redirect_chain: [],
        ip_geolocation: { lat: 37.7, lon: -122.4, country: 'US', city: 'San Francisco' }
      }
    });

    const res = await request(app)
      .get('/api/dashboard/threat-map')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('latitude');
    expect(res.body[0]).toHaveProperty('longitude');
  });

  test('I54 — threat-map filters out analyses with null ip_geolocation', async () => {
    // Create a case+analysis with NO geo data
    const noGeoCase = await prisma.case.create({
      data: {
        case_number: 'CASE-2026-NOGEO',
        userId: authUser.id,
        url: 'https://nogeo.example.com',
        source: 'Other',
        priority: 'Low',
        tags: []
      }
    });
    await prisma.analysis.create({
      data: {
        caseId: noGeoCase.id,
        threat_score: 30,
        severity: 'Low',
        redirect_chain: [],
        ip_geolocation: null
      }
    });

    const res = await request(app)
      .get('/api/dashboard/threat-map')
      .set(authHeader());

    expect(res.statusCode).toBe(200);
    // No entry should have null/undefined lat
    res.body.forEach(entry => {
      expect(entry.latitude).not.toBeNull();
      expect(entry.latitude).not.toBeUndefined();
    });
  });
});
