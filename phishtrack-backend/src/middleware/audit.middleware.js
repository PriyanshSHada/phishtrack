const prisma = require('../prismaClient');

module.exports = (actionOverride) => {
  return async (req, res, next) => {
    res.on('finish', async () => {
      try {
        const isMutation = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method);
        const isAuth = req.originalUrl.includes('/api/auth');
        
        if (isMutation || isAuth) {
          const userId = req.user?.userId || null;
          let caseId = req.params.caseId || req.params.id || req.body?.caseId || null;
          
          // Validate UUID structure for caseId
          const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
          if (caseId && !uuidRegex.test(caseId)) {
            caseId = null;
          }

          // For DELETE operations on cases, the case has already been deleted by the handler.
          // The foreign key would fail if we try to log with the now-deleted caseId. Set it to null.
          if (caseId && req.method === 'DELETE' && req.originalUrl.startsWith('/api/cases')) {
            caseId = null;
          }

          let action = actionOverride || `${req.method} ${req.originalUrl}`;
          
          // Beautify actions based on path
          if (req.originalUrl.startsWith('/api/cases')) {
            if (req.method === 'POST') action = 'CASE_CREATED';
            else if (req.method === 'PUT') action = 'CASE_UPDATED';
            else if (req.method === 'DELETE') action = 'CASE_DELETED';
          } else if (req.originalUrl.startsWith('/api/analysis/run')) {
            action = 'CASE_ANALYSIS_TRIGGERED';
          } else if (req.originalUrl.startsWith('/api/reports/generate')) {
            action = 'REPORT_GENERATION_TRIGGERED';
          } else if (req.originalUrl.startsWith('/api/auth/login')) {
            action = 'USER_LOGIN_ATTEMPT';
          } else if (req.originalUrl.startsWith('/api/auth/verify-otp')) {
            // Only mark as success when the OTP was actually accepted
            action = (res.statusCode >= 200 && res.statusCode < 300)
              ? 'USER_LOGIN_SUCCESS'
              : 'USER_LOGIN_FAILED';
          } else if (req.originalUrl.startsWith('/api/auth/register')) {
            action = 'USER_REGISTRATION';
          }

          // Prepare metadata without sensitive values (e.g. passwords)
          const bodyMetadata = { ...req.body };
          if (bodyMetadata.password) bodyMetadata.password = '***';
          if (bodyMetadata.otp) bodyMetadata.otp = '***';

          await prisma.auditLog.create({
            data: {
              userId,
              caseId,
              action,
              ip_address: req.ip || req.headers['x-forwarded-for'] || null,
              device_id: req.headers['x-device-id'] || req.user?.deviceId || null,
              metadata: {
                method: req.method,
                url: req.originalUrl,
                status: res.statusCode,
                query: req.query,
                body: req.method !== 'GET' ? bodyMetadata : undefined
              }
            }
          });
        }
      } catch (err) {
        console.error('Audit middleware logging error:', err);
      }
    });
    next();
  };
};
