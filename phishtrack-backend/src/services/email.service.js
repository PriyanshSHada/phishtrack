/**
 * Email Service using Brevo (Sendinblue) API
 */

exports.sendOtp = async (to, otp) => {
  const brevoApiKey = process.env.BREVO_API_KEY;
  const senderEmail = process.env.BREVO_SENDER_EMAIL || 'phishtrackoffical@gmail.com';

  if (!brevoApiKey) {
    console.warn(`[email stub] To: ${to} OTP: ${otp} (Brevo API Key not configured)`);
    return;
  }

  const payload = {
    sender: { email: senderEmail, name: 'PhishTrack Security' },
    to: [{ email: to }],
    subject: 'Your PhishTrack Verification Code',
    htmlContent: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
        <h2 style="color: #0A0E1A;">PhishTrack Authentication</h2>
        <p>Your one-time password (OTP) is:</p>
        <h1 style="font-size: 32px; letter-spacing: 5px; color: #00F5FF; background: #141829; padding: 10px; text-align: center; border-radius: 5px;">${otp}</h1>
        <p>This code will expire in 5 minutes. Do not share this code with anyone.</p>
        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
        <p style="font-size: 12px; color: #8892B0;">If you did not request this, please ignore this email or contact security.</p>
      </div>
    `
  };

  try {
    const response = await fetch('https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': brevoApiKey
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errorData = await response.text();
      console.error('Brevo API Error:', response.status, errorData);
      // We don't throw an error here, so the login request doesn't crash 
      // but developers can see the API key is wrong in the logs
    } else {
      console.log(`[Brevo] OTP email successfully sent to ${to}`);
    }
  } catch (error) {
    console.error('Error sending email via Brevo:', error);
  }
};
