const cron = require('node-cron');
const prisma = require('../prismaClient');
const logger = require('../utils/logger');

// Run every day at midnight
cron.schedule('0 0 * * *', async () => {
  logger.info('Starting daily cleanup of expired cases...');
  try {
    const expiredCases = await prisma.case.findMany({
      where: {
        auto_delete_at: {
          lte: new Date()
        }
      },
      select: { id: true }
    });

    if (expiredCases.length === 0) {
      logger.info('No expired cases found for deletion.');
      return;
    }

    const ids = expiredCases.map(c => c.id);

    // Delete child records first
    await prisma.analysis.deleteMany({ where: { caseId: { in: ids } } });
    await prisma.chainOfCustody.deleteMany({ where: { caseId: { in: ids } } });
    await prisma.report.deleteMany({ where: { caseId: { in: ids } } });
    await prisma.auditLog.deleteMany({ where: { caseId: { in: ids } } });
    
    // Delete the cases
    const result = await prisma.case.deleteMany({
      where: { id: { in: ids } }
    });

    logger.info(\Successfully auto-deleted \ expired cases.\);
  } catch (error) {
    logger.error('Error during auto-deletion of expired cases:', error);
  }
});

