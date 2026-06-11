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

const createCaseSchema = z.object({
  url: z.string().min(1, 'URL is required').max(2048, 'URL is too long'),
  description: z.string().max(5000).optional().default(''),
  source: z.enum(['WhatsApp', 'Email', 'SMS', 'Other']).optional().default('Other'),
  priority: z.enum(['Low', 'Medium', 'High', 'Critical']).optional().default('Low'),
  tags: z.array(z.string().max(50)).max(10).optional().default([])
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