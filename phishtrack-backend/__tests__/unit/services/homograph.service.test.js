const { detectHomographAttack } = require('../../../src/services/analysis/homograph.service');

describe('Homograph Attack Detection', () => {
  it('should detect a Cyrillic homograph of apple.com', async () => {
    // 'а' is Cyrillic U+0430
    const url = 'http://аpple.com';
    const result = await detectHomographAttack(url);
    
    expect(result.isHomograph).toBe(true);
    expect(result.confidence).toBe('high');
    expect(result.similarTo).toBe('apple');
    expect(result.detectedScript).toMatch(/Cyrillic|Mixed/);
  });

  it('should detect a Cyrillic homograph of google.com', async () => {
    // 'о' is Cyrillic U+043E
    const url = 'https://gооgle.com';
    const result = await detectHomographAttack(url);
    
    expect(result.isHomograph).toBe(true);
    expect(result.confidence).toBe('high');
    expect(result.similarTo).toBe('google');
    expect(result.detectedScript).toMatch(/Cyrillic|Mixed/);
  });

  it('should return isHomograph false for legitimate apple.com', async () => {
    const url = 'https://apple.com';
    const result = await detectHomographAttack(url);
    
    expect(result.isHomograph).toBe(false);
    expect(result.confidence).toBe('low');
    expect(result.similarTo).toBe(null);
    expect(result.detectedScript).toBe('Latin');
  });

  it('should handle URL without protocol properly', async () => {
    // 'а' is Cyrillic U+0430
    const url = 'аmazon.com/login';
    const result = await detectHomographAttack(url);
    
    expect(result.isHomograph).toBe(true);
    expect(result.confidence).toBe('high');
    expect(result.similarTo).toBe('amazon');
    expect(result.detectedScript).toMatch(/Cyrillic|Mixed/);
  });
});
