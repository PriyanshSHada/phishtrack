const prisma = require('../prismaClient');

exports.getAuditLogs = async (req, res, next) => {
  try {
    const logs = await prisma.auditLog.findMany({
      orderBy: { timestamp: 'desc' },
      include: {
        user: {
          select: {
            id: true,
            name: true,
            email: true,
            role: true
          }
        },
        case: {
          select: {
            id: true,
            case_number: true,
            url: true
          }
        }
      }
    });
    res.json(logs);
  } catch (err) {
    next(err);
  }
};

exports.getCustodyChain = async (req, res, next) => {
  try {
    const { caseId } = req.params;
    if (!caseId) {
      return res.status(400).json({ error: 'Missing caseId' });
    }
    const chain = await prisma.chainOfCustody.findMany({
      where: { caseId },
      orderBy: { timestamp: 'asc' },
      include: {
        user: {
          select: {
            id: true,
            name: true,
            email: true
          }
        },
        case: {
          select: {
            id: true,
            case_number: true
          }
        }
      }
    });
    res.json(chain);
  } catch (err) {
    next(err);
  }
};
