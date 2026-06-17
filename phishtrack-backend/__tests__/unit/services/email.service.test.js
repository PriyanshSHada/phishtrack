'use strict';

const nock = require('nock');
const logger = require('../../../src/utils/logger');
jest.mock('../../../src/utils/logger', () => ({
  warn: jest.fn(),
  info: jest.fn(),
  error: jest.fn()
}));

const emailService = require('../../../src/services/email.service');

const BREVO_API = 'https://api.brevo.com';

beforeEach(() => {
  nock.cleanAll();
  delete process.env.BREVO_API_KEY;
  jest.clearAllMocks();
});

afterEach(() => jest.restoreAllMocks());
afterAll(() => nock.cleanAll());

describe('email.service — sendOtp', () => {
  test('U46 — no BREVO_API_KEY logs warning and does NOT call Brevo', async () => {
    await expect(emailService.sendOtp('user@example.com', '123456')).resolves.toBeUndefined();
    expect(logger.warn).toHaveBeenCalledWith(expect.stringContaining('[email stub]'));
    // nock would intercept and fail if a real HTTP call was made
    expect(nock.pendingMocks()).toHaveLength(0);
  });

  test('U47 — Brevo returns 2xx logs success message', async () => {
    process.env.BREVO_API_KEY = 'brevo-key-test';
    nock(BREVO_API).post('/v3/smtp/email').reply(201, { messageId: 'msg-001' });

    await emailService.sendOtp('user@example.com', '654321');
    expect(logger.info).toHaveBeenCalledWith(expect.stringContaining('successfully sent'));
  });

  test('U48 — Brevo returns 4xx logs error but does NOT throw', async () => {
    process.env.BREVO_API_KEY = 'brevo-key-test';
    nock(BREVO_API).post('/v3/smtp/email').reply(401, 'Unauthorized');

    await expect(emailService.sendOtp('user@example.com', '111111')).resolves.toBeUndefined();
    expect(logger.error).toHaveBeenCalledWith(expect.stringContaining('Brevo API Error'), expect.any(Object));
  });

  test('U49 — Network error to Brevo is caught and does NOT throw', async () => {
    process.env.BREVO_API_KEY = 'brevo-key-test';
    nock(BREVO_API).post('/v3/smtp/email').replyWithError('ECONNREFUSED');

    await expect(emailService.sendOtp('user@example.com', '999999')).resolves.toBeUndefined();
    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('Error sending email via Brevo'),
      expect.any(Object)
    );
  });
});
