'use strict';

process.env.JWT_SECRET = 'test-jwt-secret-at-least-32-characters-long';

const authMiddleware = require('../../../src/middleware/auth.middleware');
const { signAccessToken } = require('../../../src/utils/jwt.util');
const jwt = require('jsonwebtoken');

// Helper to build mock req/res/next
function makeReqRes(headers = {}) {
  const req = { headers };
  const res = {
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis()
  };
  const next = jest.fn();
  return { req, res, next };
}

describe('auth.middleware', () => {
  test('U50 — missing Authorization header returns 401', async () => {
    const { req, res, next } = makeReqRes({});
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Missing authorization header' });
    expect(next).not.toHaveBeenCalled();
  });

  test('U51 — non-Bearer format returns 401', async () => {
    const { req, res, next } = makeReqRes({ authorization: 'Basic abc123' });
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Invalid authorization format' });
    expect(next).not.toHaveBeenCalled();
  });

  test('U51b — Bearer with no token (only one part) returns 401', async () => {
    const { req, res, next } = makeReqRes({ authorization: 'Bearer' });
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
  });

  test('U52 — garbage token string returns 401 Invalid token', async () => {
    const { req, res, next } = makeReqRes({ authorization: 'Bearer notavalidjwt' });
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Invalid token' });
  });

  test('U53 — valid token sets req.user and calls next()', async () => {
    const token = signAccessToken({ userId: 'user-001' });
    const { req, res, next } = makeReqRes({ authorization: `Bearer ${token}` });
    await authMiddleware(req, res, next);
    expect(next).toHaveBeenCalled();
    expect(req.user).toBeDefined();
    expect(req.user.userId).toBe('user-001');
  });

  test('U54 — expired token returns 401 Invalid token', async () => {
    const expired = jwt.sign({ userId: 'user-001' }, process.env.JWT_SECRET, { expiresIn: -1 });
    const { req, res, next } = makeReqRes({ authorization: `Bearer ${expired}` });
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Invalid token' });
  });
});
