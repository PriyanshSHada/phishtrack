'use strict';

// Mock the tls module before requiring the service
const mockSocket = {
  getPeerCertificate: jest.fn(),
  destroy: jest.fn(),
  setTimeout: jest.fn(),
  on: jest.fn(),
};

jest.mock('tls', () => ({
  connect: jest.fn((port, host, options, callback) => {
    // Invoke callback asynchronously to simulate connection
    setImmediate(callback);
    return mockSocket;
  })
}));

const tls = require('tls');
const sslService = require('../../../src/services/analysis/ssl.service');

// Build a valid future certificate
function makeCert(overrides = {}) {
  const now = new Date();
  return {
    subject: { CN: 'example.com' },
    issuer: { O: "Let's Encrypt" },
    valid_from: new Date(now.getTime() - 86400000).toUTCString(), // yesterday
    valid_to: new Date(now.getTime() + 86400000 * 365).toUTCString(), // +1 year
    fingerprint: 'AA:BB:CC',
    serialNumber: '1234567890ABCDEF',
    ...overrides
  };
}

beforeEach(() => {
  jest.clearAllMocks();
  // Reset default mock behavior: a healthy on('error') that does nothing
  mockSocket.on.mockImplementation(() => mockSocket);
});

describe('ssl.service', () => {
  test('U23 — HTTP URL returns { valid: false, error: Protocol is not HTTPS }', async () => {
    const result = await sslService.getSslInfo('http://example.com');
    expect(result).toEqual({ valid: false, error: 'Protocol is not HTTPS' });
    expect(tls.connect).not.toHaveBeenCalled();
  });

  test('U24 — valid non-expired cert returns { valid: true } with cert details', async () => {
    const cert = makeCert();
    mockSocket.getPeerCertificate.mockReturnValue(cert);
    const result = await sslService.getSslInfo('https://example.com');
    expect(result.valid).toBe(true);
    expect(result.subject).toEqual(cert.subject);
    expect(result.issuer).toEqual(cert.issuer);
    expect(result.fingerprint).toBe('AA:BB:CC');
  });

  test('U25 — expired cert (validTo in the past) returns { valid: false }', async () => {
    const now = new Date();
    const cert = makeCert({
      valid_to: new Date(now.getTime() - 1000).toUTCString() // 1 second ago
    });
    mockSocket.getPeerCertificate.mockReturnValue(cert);
    const result = await sslService.getSslInfo('https://expired.example.com');
    expect(result.valid).toBe(false);
  });

  test('U26 — timeout fires and returns { valid: false, error: Connection timed out }', async () => {
    mockSocket.getPeerCertificate.mockReturnValue(makeCert());
    // Simulate timeout by triggering the timeout callback
    mockSocket.on.mockImplementation((event, cb) => {
      if (event === 'timeout') setImmediate(cb);
      return mockSocket;
    });
    // Override tls.connect so callback is never called (simulating hung connect)
    tls.connect.mockImplementation((port, host, options, callback) => {
      // Don't call callback — simulate a hung connection
      return mockSocket;
    });

    const result = await sslService.getSslInfo('https://hangs.example.com');
    expect(result).toEqual({ valid: false, error: 'Connection timed out' });
  });

  test('U27 — socket error event returns { valid: false, error: <message> }', async () => {
    mockSocket.on.mockImplementation((event, cb) => {
      if (event === 'error') setImmediate(() => cb(new Error('ECONNREFUSED')));
      return mockSocket;
    });
    tls.connect.mockImplementation((port, host, options, callback) => {
      return mockSocket;
    });

    const result = await sslService.getSslInfo('https://refused.example.com');
    expect(result).toEqual({ valid: false, error: 'ECONNREFUSED' });
  });

  test('U28 — empty cert object returns { valid: false, error: No certificate found }', async () => {
    mockSocket.getPeerCertificate.mockReturnValue({});
    // Reset tls.connect to call callback
    tls.connect.mockImplementation((port, host, options, callback) => {
      setImmediate(callback);
      return mockSocket;
    });
    const result = await sslService.getSslInfo('https://nocert.example.com');
    expect(result).toEqual({ valid: false, error: 'No certificate found' });
  });

  test('U29 — socket.destroy() is called (not socket.end) after getting cert', async () => {
    const cert = makeCert();
    mockSocket.getPeerCertificate.mockReturnValue(cert);
    tls.connect.mockImplementation((port, host, options, callback) => {
      setImmediate(callback);
      return mockSocket;
    });
    await sslService.getSslInfo('https://example.com');
    expect(mockSocket.destroy).toHaveBeenCalled();
  });
});
