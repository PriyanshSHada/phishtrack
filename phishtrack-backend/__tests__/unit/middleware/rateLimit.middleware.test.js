'use strict';

// ── Redis mock ────────────────────────────────────────────────────────────────
let store = {};
let isOpen = true;

const mockRedis = {
  get isOpen() { return isOpen; },
  incr: jest.fn(async (key) => {
    store[key] = (store[key] || 0) + 1;
    return store[key];
  }),
  expire: jest.fn().mockResolvedValue(1),
  ttl: jest.fn().mockResolvedValue(600),
  get: jest.fn(async (key) => store[key] ?? null),
  setEx: jest.fn().mockResolvedValue('OK'),
  del: jest.fn().mockResolvedValue(1),
  on: jest.fn(),
  connect: jest.fn().mockResolvedValue(undefined),
};

jest.mock('../../../src/redisClient', () => mockRedis);

const rateLimitMiddleware = require('../../../src/middleware/rateLimit.middleware');

function makeReqRes() {
  const req = { user: undefined, ip: '127.0.0.1', headers: {} };
  const res = {
    setHeader: jest.fn(),
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis()
  };
  const next = jest.fn();
  return { req, res, next };
}

beforeEach(() => {
  store = {};
  isOpen = true;
  jest.clearAllMocks();
  mockRedis.incr.mockImplementation(async (key) => {
    store[key] = (store[key] || 0) + 1;
    return store[key];
  });
  mockRedis.ttl.mockResolvedValue(600);
  mockRedis.expire.mockResolvedValue(1);
});

describe('rateLimit.middleware', () => {
  const limiter = rateLimitMiddleware({ windowMs: 60_000, max: 3, keyPrefix: 'rl:test:' });

  test('U72 — Redis not connected calls next() (fail-open)', async () => {
    isOpen = false;
    const { req, res, next } = makeReqRes();
    await limiter(req, res, next);
    expect(next).toHaveBeenCalled();
    expect(res.status).not.toHaveBeenCalled();
  });

  test('U73 — first request sets TTL and returns correct rate-limit headers', async () => {
    const { req, res, next } = makeReqRes();
    await limiter(req, res, next);
    expect(next).toHaveBeenCalled();
    expect(res.setHeader).toHaveBeenCalledWith('X-RateLimit-Limit', 3);
    expect(res.setHeader).toHaveBeenCalledWith('X-RateLimit-Remaining', 2); // max - current(1)
    expect(mockRedis.expire).toHaveBeenCalled();
  });

  test('U74 — request count equal to max still passes (limit not exceeded)', async () => {
    // Simulate 3rd request (count equals max)
    store['rl:test:127.0.0.1'] = 2; // pre-populate so incr returns 3
    const { req, res, next } = makeReqRes();
    await limiter(req, res, next);
    expect(next).toHaveBeenCalled();
    expect(res.status).not.toHaveBeenCalled();
  });

  test('U75 — request count exceeds max returns 429 with retryAfter', async () => {
    // Simulate 4th request (count > max of 3)
    store['rl:test:127.0.0.1'] = 3;
    const { req, res, next } = makeReqRes();
    await limiter(req, res, next);
    expect(res.status).toHaveBeenCalledWith(429);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ retryAfter: expect.any(Number) }));
    expect(next).not.toHaveBeenCalled();
  });

  test('U76 — Redis incr error causes fail-open (next is called)', async () => {
    mockRedis.incr.mockRejectedValueOnce(new Error('Connection lost'));
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const { req, res, next } = makeReqRes();
    await limiter(req, res, next);
    expect(next).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  test('userId-based key is preferred over IP when req.user is set', async () => {
    const { req, res, next } = makeReqRes();
    req.user = { userId: 'user-abc' };
    await limiter(req, res, next);
    expect(next).toHaveBeenCalled();
    // The key should include userId, not IP
    expect(store['rl:test:user-abc']).toBe(1);
    expect(store['rl:test:127.0.0.1']).toBeUndefined();
  });
});
