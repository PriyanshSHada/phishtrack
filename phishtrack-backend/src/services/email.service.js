const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: process.env.SMTP_PORT,
  secure: process.env.SMTP_PORT == 465, // true for 465, false for other ports
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

exports.sendOtp = async (to, otp, options = {}) => {
  const subject = 'Your PhishTrack verification code';
  const text = `Your verification code is: ${otp}`;
  const from = process.env.SMTP_USER || 'noreply@phishtrack.com';

  console.log(`[OTP Sent] To: ${to} OTP: ${otp}`);
  
  if (!process.env.SMTP_HOST || !process.env.SMTP_USER) {
    console.warn(`[email stub] To: ${to} OTP: ${otp} (SMTP not configured)`);
    return;
  }

  const payload = { from, to, subject, text };
  if (options.replyTo) payload.replyTo = options.replyTo;

  try {
    const info = await transporter.sendMail(payload);
    console.log(`[Nodemailer] Email sent: ${info.messageId}`);
  } catch (err) {
    console.error(`[Nodemailer Error] Failed to send email to ${to}:`, err);
  }
};
