'use strict';

const axios = require('axios');

jest.mock('axios');

const sandboxService = require('../../../src/services/sandbox/puppeteer.service');

beforeEach(() => {
  jest.clearAllMocks();
  delete process.env.SCREENSHOTONE_API_KEY;
});

describe('puppeteer.service', () => {
  test('returns a screenshot from ScreenshotOne when the API key is configured', async () => {
    process.env.SCREENSHOTONE_API_KEY = 'sk-test-screenshot-one';
    axios.get
      .mockResolvedValueOnce({
        status: 200,
        data: '<html><body>ok</body></html>',
        headers: {}
      })
      .mockResolvedValueOnce({
        data: Buffer.from('png-primary')
      });

    const result = await sandboxService.runSandbox('https://example.com/path?q=hello world');

    expect(result.error).toBeUndefined();
    expect(result.screenshot).toMatch(/^data:image\/png;base64,/);
    expect(axios.get).toHaveBeenCalledTimes(2);
    expect(axios.get.mock.calls[1][0]).toContain('api.screenshotone.com/take');
  });

  test('falls back to thum.io and encodes the target URL when ScreenshotOne fails', async () => {
    process.env.SCREENSHOTONE_API_KEY = 'sk-test-screenshot-one';
    axios.get
      .mockResolvedValueOnce({
        status: 200,
        data: '<html><body>ok</body></html>',
        headers: {}
      })
      .mockRejectedValueOnce(new Error('ScreenshotOne unavailable'))
      .mockResolvedValueOnce({
        data: Buffer.from('png-fallback')
      });

    const targetUrl = 'https://example.com/path?q=hello world&next=/login';
    const result = await sandboxService.runSandbox(targetUrl);

    expect(result.error).toBeUndefined();
    expect(result.screenshot).toMatch(/^data:image\/png;base64,/);
    expect(axios.get).toHaveBeenCalledTimes(3);
    expect(axios.get.mock.calls[2][0]).toContain('image.thum.io/get/width/1280/crop/720/');
    expect(axios.get.mock.calls[2][0]).toContain('hello%20world');
    expect(axios.get.mock.calls[2][0]).toContain('%2Flogin');
  });
});