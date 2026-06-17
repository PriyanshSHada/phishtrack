'use strict';

const { generateCaseNumber } = require('../../../src/utils/caseNumber.util');

describe('caseNumber.util', () => {
  const date2026 = new Date('2026-06-17');

  test('U1 — seq 1 produces CASE-YYYY-001', () => {
    expect(generateCaseNumber(1, date2026)).toBe('CASE-2026-001');
  });

  test('U2 — seq 99 produces CASE-YYYY-099', () => {
    expect(generateCaseNumber(99, date2026)).toBe('CASE-2026-099');
  });

  test('U3 — seq 1000 (overflow) is not truncated', () => {
    expect(generateCaseNumber(1000, date2026)).toBe('CASE-2026-1000');
  });

  test('U4 — uses the year from the passed date argument', () => {
    const date2020 = new Date('2020-01-01');
    expect(generateCaseNumber(5, date2020)).toBe('CASE-2020-005');
  });

  test('U4b — defaults to current year when no date given', () => {
    const currentYear = new Date().getFullYear();
    expect(generateCaseNumber(1)).toBe(`CASE-${currentYear}-001`);
  });
});
