'use strict';

// Mock prisma before requiring the middleware
const mockCreate = jest.fn().mockResolvedValue({});
jest.mock('../../../src/prismaClient', () => ({
  auditLog: { create: (...args) => mockCreate(...args) }
}));

const logger = require('../../../src/utils/logger');
jest.mock('../../../src/utils/logger', () => ({
  error: jest.fn()
}));

const auditMiddleware = require('../../../src/middleware/audit.middleware');

/** Helper: builds req/res/next, invokes the middleware, and optionally fires 'finish' */
function setup({ method = 'POST', url = '/api/cases', params = {}, body = {}, user = { userId: 'u1' }, statusCode = 201 } = {}) {
  let finishCb;
  const res = {
    statusCode,
    on: jest.fn((event, cb) => { if (event === 'finish') finishCb = cb; }),
  };
  const req = { method, originalUrl: url, params, body, user, ip: '127.0.0.1', headers: {} };
  const next = jest.fn();

  const middleware = auditMiddleware();
  middleware(req, res, next);

  return { req, res, next, fireFinish: async () => { await finishCb?.(); } };
}

beforeEach(() => { mockCreate.mockClear(); });

describe('audit.middleware', () => {
  test('U55 — GET request does NOT create an audit log', async () => {
    const { fireFinish } = setup({ method: 'GET', url: '/api/cases' });
    await fireFinish();
    expect(mockCreate).not.toHaveBeenCalled();
  });

  test('U56 — POST to /api/cases creates log with action CASE_CREATED', async () => {
    const { fireFinish } = setup({ method: 'POST', url: '/api/cases' });
    await fireFinish();
    expect(mockCreate).toHaveBeenCalledTimes(1);
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.action).toBe('CASE_CREATED');
  });

  test('U57 — DELETE to /api/cases/:id sets caseId to null (prevents FK error)', async () => {
    const { fireFinish } = setup({
      method: 'DELETE',
      url: '/api/cases/abc-123',
      params: { id: 'f47ac10b-58cc-4372-a567-0e02b2c3d479' }
    });
    await fireFinish();
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.caseId).toBeNull();
  });

  test('U58 — POST to /api/auth/verify-otp with 200 logs USER_LOGIN_SUCCESS', async () => {
    const { fireFinish } = setup({ method: 'POST', url: '/api/auth/verify-otp', statusCode: 200 });
    await fireFinish();
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.action).toBe('USER_LOGIN_SUCCESS');
  });

  test('U59 — POST to /api/auth/verify-otp with 400 logs USER_LOGIN_FAILED', async () => {
    const { fireFinish } = setup({ method: 'POST', url: '/api/auth/verify-otp', statusCode: 400 });
    await fireFinish();
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.action).toBe('USER_LOGIN_FAILED');
  });

  test('U60 — non-UUID caseId in params is set to null', async () => {
    const { fireFinish } = setup({
      method: 'PUT',
      url: '/api/cases/not-a-uuid',
      params: { id: 'not-a-uuid' }
    });
    await fireFinish();
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.caseId).toBeNull();
  });

  test('U61 — password field in body is masked as ***', async () => {
    const { fireFinish } = setup({
      method: 'POST',
      url: '/api/auth/login',
      body: { email: 'a@b.com', password: 'supersecret' }
    });
    await fireFinish();
    const data = mockCreate.mock.calls[0][0].data;
    expect(data.metadata.body.password).toBe('***');
    expect(data.metadata.body.email).toBe('a@b.com');
  });

  test('U62 — Prisma write failure is caught; next() is NOT affected', async () => {
    mockCreate.mockRejectedValueOnce(new Error('DB error'));
    const { next, fireFinish } = setup({ method: 'POST', url: '/api/cases' });

    expect(next).toHaveBeenCalled(); // next() called synchronously before finish
    await fireFinish();
    expect(logger.error).toHaveBeenCalledWith(expect.stringContaining('Audit middleware logging error'), expect.any(Object));
  });
});
