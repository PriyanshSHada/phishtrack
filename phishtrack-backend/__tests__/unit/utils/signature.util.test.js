'use strict';

const { sign, verify } = require('../../../src/utils/signature.util');

const samplePayload = {
  caseId: 'abc-123',
  caseNumber: 'CASE-2026-001',
  threatScore: 75,
  severity: 'High',
  version: 1,
  generatedById: 'user-xyz',
  generatedAt: '2026-06-17T00:00:00.000Z'
};

describe('signature.util', () => {
  test('U5 — sign() returns a 64-character hex string', () => {
    const sig = sign(samplePayload);
    expect(sig).toMatch(/^[0-9a-f]{64}$/);
  });

  test('U6 — sign() is deterministic for identical input', () => {
    expect(sign(samplePayload)).toBe(sign(samplePayload));
  });

  test('U7 — verify() returns true for a matching signature', () => {
    const sig = sign(samplePayload);
    expect(verify(samplePayload, sig)).toBe(true);
  });

  test('U8 — verify() returns false for a tampered signature string', () => {
    expect(verify(samplePayload, 'a'.repeat(64))).toBe(false);
  });

  test('U9 — verify() returns false when payload differs from signed payload', () => {
    const sig = sign(samplePayload);
    const tamperedPayload = { ...samplePayload, version: 99 };
    expect(verify(tamperedPayload, sig)).toBe(false);
  });

  test('U10 — verify() returns false (no crash) for wrong-length signature', () => {
    expect(verify(samplePayload, 'tooshort')).toBe(false);
  });

  test('U10b — verify() returns false for empty string signature', () => {
    expect(verify(samplePayload, '')).toBe(false);
  });
});
