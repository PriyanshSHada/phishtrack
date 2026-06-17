'use strict';

const { checkSimilarity } = require('../../../src/services/analysis/domainCheck.service');

describe('domainCheck.service — checkSimilarity', () => {
  test('U30 — exact brand domain returns isBrandDomain: true', () => {
    const result = checkSimilarity('https://google.com/search');
    expect(result.isBrandDomain).toBe(true);
    expect(result.distance).toBe(0);
    expect(result.similarTo).toBeNull();
  });

  test('U30b — www prefix ignored; still recognised as brand', () => {
    const result = checkSimilarity('https://www.paypal.com');
    expect(result.isBrandDomain).toBe(true);
  });

  test('U31 — typosquatting (g00gle.com, distance ≤ 2) is flagged', () => {
    const result = checkSimilarity('https://g00gle.com');
    expect(result.isBrandDomain).toBe(false);
    expect(result.reason).toBe('typosquatting');
    expect(result.similarTo).toBe('google.com');
    expect(result.distance).toBeGreaterThan(0);
    expect(result.distance).toBeLessThanOrEqual(2);
  });

  test('U32 — domain containing brand name is flagged with contains_brand_name', () => {
    const result = checkSimilarity('https://login-paypal.com/signin');
    expect(result.isBrandDomain).toBe(false);
    expect(result.reason).toBe('contains_brand_name');
    expect(result.similarTo).toBe('paypal.com');
  });

  test('U33 — completely unrelated domain returns similarTo: null', () => {
    const result = checkSimilarity('https://zyxwvutsrqponm.xyz');
    expect(result.isBrandDomain).toBe(false);
    expect(result.similarTo).toBeNull();
  });

  test('U34 — invalid URL string returns error object without throwing', () => {
    const result = checkSimilarity('not-a-valid-url');
    expect(result).toBeDefined();
    expect(result.error).toBeDefined();
  });

  test('U34b — another typosquatting example: facebok.com', () => {
    const result = checkSimilarity('https://facebok.com');
    expect(result.isBrandDomain).toBe(false);
    expect(result.reason).toBe('typosquatting');
    expect(result.similarTo).toBe('facebook.com');
  });
});
