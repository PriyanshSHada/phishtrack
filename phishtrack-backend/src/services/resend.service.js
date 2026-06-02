const { Resend } = require('resend');

const API_KEY = process.env.RESEND_API_KEY;
const FROM = process.env.RESEND_FROM || 'onboarding@resend.dev';
const REPLY_TO = process.env.RESEND_REPLY_TO || null;
const resend = API_KEY ? new Resend(API_KEY) : null;

exports.sendOtp = async (to, otp, options = {}) => {
  const subject = 'Your PhishTrack verification code';
  const text = `Your verification code is: ${otp}`;
  const replyTo = options.replyTo || REPLY_TO;

  if (!resend) {
    console.log(`[resend stub] To: ${to} OTP: ${otp} Reply-To: ${replyTo || 'none'}`);
    return;
  }

  const payload = { from: FROM, to, subject, text };
  if (replyTo) payload.reply_to = replyTo;

  await resend.emails.send(payload);
};
