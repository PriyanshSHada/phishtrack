'use strict';

// JWT_SECRET must be set before the module is loaded
process.env.JWT_SECRET = 'test-jwt-secret-at-least-32-characters-long';

const { signAccessToken, signRefreshToken, signJwt, verifyJwt } = require('../../../src/utils/jwt.util');
const jwt = require('jsonwebtoken');

describe('jwt.util', () => {
  const payload = { userId: 'user-001' };

  // ── signAccessToken ─────────────────────────────────────────────────────────
  describe('signAccessToken', () => {
    test('U11 — produces a token verifiable by verifyJwt with correct payload', async () => {
      const token = signAccessToken(payload);
      const decoded = await verifyJwt(token);
      expect(decoded).toBeTruthy();
      expect(decoded.userId).toBe(payload.userId);
    });

    test('U12 — access token expires in ~15 minutes (900 seconds)', () => {
      const token = signAccessToken(payload);
      const decoded = jwt.decode(token);
      const diff = decoded.exp - decoded.iat;
      // Allow 5-second tolerance for test execution time
      expect(diff).toBeGreaterThanOrEqual(895);
      expect(diff).toBeLessThanOrEqual(905);
    });
  });

  // ── signRefreshToken ────────────────────────────────────────────────────────
  describe('signRefreshToken', () => {
    test('U13 — refresh token expires in ~7 days (604800 seconds)', () => {
      const token = signRefreshToken(payload);
      const decoded = jwt.decode(token);
      const diff = decoded.exp - decoded.iat;
      expect(diff).toBeGreaterThanOrEqual(604795);
      expect(diff).toBeLessThanOrEqual(604805);
    });
  });

  // ── verifyJwt ───────────────────────────────────────────────────────────────
  describe('verifyJwt', () => {
    test('U14 — returns null for an expired token', async () => {
      const expired = jwt.sign(payload, process.env.JWT_SECRET, { expiresIn: -1 });
      expect(await verifyJwt(expired)).toBeNull();
    });

    test('U15 — returns null for a garbage string', async () => {
      expect(await verifyJwt('not.a.jwt')).toBeNull();
    });

    test('U15b — returns null for empty string', async () => {
      expect(await verifyJwt('')).toBeNull();
    });
  });

  // ── signJwt backward-compat alias ──────────────────────────────────────────
  describe('signJwt (backward-compat alias)', () => {
    test('signJwt produces a valid token (7d expiry)', async () => {
      const token = signJwt(payload);
      const decoded = await verifyJwt(token);
      expect(decoded).toBeTruthy();
      expect(decoded.userId).toBe(payload.userId);
    });
  });
});
