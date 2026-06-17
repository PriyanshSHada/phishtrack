'use strict';

const { validate, schemas } = require('../../../src/middleware/validation.middleware');

/** Helper: synthesise an Express-like req/res/next and run validate(schema) */
function runValidation(schema, body) {
  const req = { body };
  const res = {
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis()
  };
  const next = jest.fn();
  validate(schema)(req, res, next);
  return { req, res, next };
}

describe('validation.middleware — register', () => {
  const valid = { email: 'user@example.com', password: 'password123' };

  test('U63 — valid register body passes and sanitises req.body', () => {
    const { next, req } = runValidation(schemas.register, valid);
    expect(next).toHaveBeenCalled();
    expect(req.body.email).toBe('user@example.com');
  });

  test('U64 — invalid email format returns 400 with field: email', () => {
    const { res, next } = runValidation(schemas.register, { email: 'not-an-email', password: 'password123' });
    expect(next).not.toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(400);
    const body = res.json.mock.calls[0][0];
    expect(body.details.some(d => d.field === 'email')).toBe(true);
  });

  test('U65 — password too short (< 8 chars) returns 400', () => {
    const { res } = runValidation(schemas.register, { email: 'user@example.com', password: 'abc' });
    expect(res.status).toHaveBeenCalledWith(400);
    const body = res.json.mock.calls[0][0];
    expect(body.details.some(d => d.field === 'password')).toBe(true);
  });
});

describe('validation.middleware — verifyOtp', () => {
  test('U66 — OTP of 5 digits fails validation', () => {
    const { res } = runValidation(schemas.verifyOtp, { email: 'a@b.com', otp: '12345' });
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('U67 — OTP of 6 letters (not digits) fails validation', () => {
    const { res } = runValidation(schemas.verifyOtp, { email: 'a@b.com', otp: 'ABCDEF' });
    expect(res.status).toHaveBeenCalledWith(400);
    const body = res.json.mock.calls[0][0];
    expect(body.details.some(d => /only digits/i.test(d.message))).toBe(true);
  });

  test('U68 — missing both email and userId fails validation', () => {
    const { res } = runValidation(schemas.verifyOtp, { otp: '123456' });
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('U68b — providing userId (no email) passes validation', () => {
    // Must be a valid UUID v4 to satisfy z.string().uuid()
    const { next } = runValidation(schemas.verifyOtp, { userId: 'f47ac10b-58cc-4372-a567-0e02b2c3d479', otp: '654321' });
    expect(next).toHaveBeenCalled();
  });
});

describe('validation.middleware — createCase', () => {
  test('U69 — URL exceeding 2048 chars fails validation', () => {
    const { res } = runValidation(schemas.createCase, { url: 'https://' + 'a'.repeat(2050) });
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('U69b — valid createCase with all optional fields passes', () => {
    const { next } = runValidation(schemas.createCase, {
      url: 'https://example.com',
      description: 'test',
      source: 'Email',
      priority: 'High',
      tags: ['phishing', 'urgent']
    });
    expect(next).toHaveBeenCalled();
  });

  test('tags array of 11 items (over limit of 10) fails validation', () => {
    const { res } = runValidation(schemas.createCase, {
      url: 'https://example.com',
      tags: Array(11).fill('tag')
    });
    expect(res.status).toHaveBeenCalledWith(400);
  });
});

describe('validation.middleware — updateCase', () => {
  test('U70 — empty body (no valid fields) fails validation', () => {
    const { res } = runValidation(schemas.updateCase, {});
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('U70b — valid status-only update passes', () => {
    const { next } = runValidation(schemas.updateCase, { status: 'Closed' });
    expect(next).toHaveBeenCalled();
  });
});

describe('validation.middleware — field stripping', () => {
  test('U71 — extra unknown fields are stripped from req.body', () => {
    const { req, next } = runValidation(schemas.login, {
      email: 'user@example.com',
      password: 'password123',
      injectedField: 'evil-value'
    });
    expect(next).toHaveBeenCalled();
    expect(req.body.injectedField).toBeUndefined();
  });
});
