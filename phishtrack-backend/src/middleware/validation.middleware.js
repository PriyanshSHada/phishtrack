const { z } = require('zod');

/**
 * Generic middleware factory that validates req.body against a Zod schema.
 * Strips any fields not present in the schema (safe parse) and
 * returns 400 with detailed errors on failure.
 */
function validate(schema) {
  return (req, res, next) => {
    const result = schema.safeParse(req.body);
    if (!result.success) {
      const errors = result.error.issues.map(i => ({
        field: i.path.join('.'),
        message: i.message
      }));
      return res.status(400).json({ error: 'Validation failed', details: errors });
    }
    req.body = result.data; // sanitized
    next();
  };
}

// ---- Auth Schemas ----

const registerSchema = z.object({
  email: z
    .string()
    .email('Invalid email address')
    .max(255, 'Email must be 255 characters or fewer'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password must be 128 characters or fewer'),
  name: z.string().max(100).optional(),
  organization: z.string().max(200).optional()
});

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required')
});

const verifyOtpSchema = z.object({
  email: z.string().email().optional(),
  userId: z.string().uuid().optional(),
  otp: z.string().length(6, 'OTP must be 6 digits').regex(/^\d{6}$/, 'OTP must contain only digits')
}).refine(data => data.email || data.userId, {
  message: 'Either email or userId is required'
});

const resendOtpSchema = z.object({
  email: z.string().email('Invalid email address')
});

// ---- Case Schemas ----

const ipv4Regex = /^(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)$/;
const ipv6Regex = /^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$/;

function isValidIp(value) {
  return ipv4Regex.test(value) || ipv6Regex.test(value);
}

const createCaseSchema = z.object({
  title: z.string().min(1, 'Title is required').max(200).optional().default('Untitled Case'),
  target_type: z.enum(['URL', 'IP']).optional().default('URL'),
  url: z.string().max(2048, 'URL is too long').optional(),
  target_ip: z.string().max(45).optional(),
  description: z.string().max(5000).optional().default(''),
  source: z.enum(['WhatsApp', 'Email', 'SMS', 'Other']).optional().default('Other'),
  priority: z.enum(['Low', 'Medium', 'High', 'Critical']).optional().default('Low'),
  tags: z.array(z.string().max(50)).max(10).optional().default([])
}).superRefine((data, ctx) => {
  const type = data.target_type || 'URL';
  if (type === 'URL') {
    if (!data.url || data.url.trim().length === 0) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'URL is required for URL cases', path: ['url'] });
    }
  } else if (!data.target_ip || !isValidIp(data.target_ip.trim())) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Valid IP address is required for IP cases', path: ['target_ip'] });
  }
});

const updateCaseSchema = z.object({
  status: z.enum(['Open', 'Investigating', 'Closed', 'False_Positive']).optional(),
  priority: z.enum(['Low', 'Medium', 'High', 'Critical']).optional(),
  description: z.string().max(5000).optional()
}).refine(data => data.status !== undefined || data.priority !== undefined || data.description !== undefined, {
  message: 'At least one of status, priority, or description is required'
});

// ---- Analysis Schema ----

const runAnalysisSchema = z.object({
  caseId: z.string().uuid('Invalid case ID')
});

// ---- Report Schema ----

const generateReportSchema = z.object({
  caseId: z.string().uuid('Invalid case ID')
});

module.exports = {
  validate,
  schemas: {
    register: registerSchema,
    login: loginSchema,
    verifyOtp: verifyOtpSchema,
    resendOtp: resendOtpSchema,
    createCase: createCaseSchema,
    updateCase: updateCaseSchema,
    runAnalysis: runAnalysisSchema,
    generateReport: generateReportSchema
  }
};