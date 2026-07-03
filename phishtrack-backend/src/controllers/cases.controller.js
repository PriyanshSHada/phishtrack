const prisma = require('../prismaClient');
const { generateCaseNumber } = require('../utils/caseNumber.util');

exports.getAllCases = async (req, res, next) => {
  try {
    const { status, priority, date, page, limit } = req.query;
    const where = { userId: req.user.userId };
    if (status) {
      if (status === 'Open') {
        where.status = { in: ['Open', 'Investigating'] };
      } else {
        where.status = status;
      }
    }
    if (priority) where.priority = priority;
    if (date) {
      const startOfDay = new Date(date);
      const endOfDay = new Date(date);
      endOfDay.setDate(endOfDay.getDate() + 1);
      where.created_at = {
        gte: startOfDay,
        lt: endOfDay
      };
    }

    // Pagination
    const pageNum = Math.max(1, parseInt(page) || 1);
    const pageSize = Math.min(100, Math.max(1, parseInt(limit) || 20));
    const skip = (pageNum - 1) * pageSize;

    const [cases, total] = await Promise.all([
      prisma.case.findMany({
        where,
        orderBy: { created_at: 'desc' },
        skip,
        take: pageSize
      }),
      prisma.case.count({ where })
    ]);

    res.json({
      data: cases,
      pagination: {
        page: pageNum,
        limit: pageSize,
        total,
        pages: Math.ceil(total / pageSize)
      }
    });
  } catch (err) {
    next(err);
  }
};

exports.createCase = async (req, res, next) => {
  try {
    const { title, description, url, target_ip, target_type, source, priority, tags } = req.body;
    const type = target_type || 'URL';

    const userId = req.user?.userId;
    if (!userId) return res.status(401).json({ error: 'Authentication required' });

    if (type === 'URL' && !url) return res.status(400).json({ error: 'Missing url' });
    if (type === 'IP' && !target_ip) return res.status(400).json({ error: 'Missing target_ip' });

    // Wrap count + create in a transaction to prevent duplicate case numbers
    // under concurrent requests.
    const created = await prisma.$transaction(async (tx) => {
      const startOfYear = new Date(new Date().getFullYear(), 0, 1);
      
      // Get the last created case this year to find the highest sequence
      const lastCase = await tx.case.findFirst({
        where: { created_at: { gte: startOfYear } },
        orderBy: { created_at: 'desc' }
      });
      
      let seq = 0;
      if (lastCase && lastCase.case_number) {
        const parts = lastCase.case_number.split('-');
        if (parts.length === 3) {
          seq = parseInt(parts[2], 10) || 0;
        }
      }
      
      const caseNumber = generateCaseNumber(seq + 1);

      return tx.case.create({
        data: {
          case_number: caseNumber,
          userId,
          title: title || 'Untitled Case',
          target_type: type,
          url: type === 'URL' ? url : null,
          target_ip: type === 'IP' ? target_ip.trim() : null,
          description: description || '',
          source: source || 'Other',
          priority: priority || 'Low',
          tags: Array.isArray(tags) ? tags : []
        }
      });
    });

    res.status(201).json(created);
  } catch (err) {
    // Handle unique constraint violation gracefully
    if (err.code === 'P2002' && err.meta?.target?.includes('case_number')) {
      return res.status(409).json({ error: 'Case number conflict — please retry.' });
    }
    next(err);
  }
};

exports.getCaseById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const c = await prisma.case.findUnique({ where: { id }, include: { analyses: true, reports: true, auditLogs: true } });
    if (!c) return res.status(404).json({ error: 'Not found' });
    if (c.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });
    res.json(c);
  } catch (err) {
    next(err);
  }
};

exports.updateCase = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status, priority, description } = req.body;

    // Verify case exists before attempting update
    const existing = await prisma.case.findUnique({ where: { id } });
    if (!existing) return res.status(404).json({ error: 'Not found' });
    if (existing.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    const validStatuses = ['Open', 'Investigating', 'Closed', 'False_Positive'];
    const validPriorities = ['Low', 'Medium', 'High', 'Critical'];
    const data = {};

    if (status !== undefined) {
      if (validStatuses.includes(status)) {
        data.status = status;
      } else {
        return res.status(400).json({ error: `Invalid status value. Must be one of: ${validStatuses.join(', ')}` });
      }
    }

    if (priority !== undefined && priority !== null) {
      if (validPriorities.includes(priority)) {
        data.priority = priority;
      } else {
        return res.status(400).json({ error: `Invalid priority value. Must be one of: ${validPriorities.join(', ')}` });
      }
    }
    if (description !== undefined) {
      data.description = description;
    }

    if (Object.keys(data).length === 0) {
      return res.status(400).json({ error: 'No valid fields to update. Use status, priority, and/or description.' });
    }

    const updated = await prisma.case.update({ where: { id }, data });
    res.json(updated);
  } catch (err) {
    next(err);
  }
};

exports.updateRetention = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { autoDelete } = req.body;

    const existing = await prisma.case.findUnique({ where: { id } });
    if (!existing) return res.status(404).json({ error: 'Not found' });
    if (existing.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    let auto_delete_at = null;
    if (autoDelete === true) {
      // 30 days from now
      const date = new Date();
      date.setDate(date.getDate() + 30);
      auto_delete_at = date;
    }

    const updated = await prisma.case.update({
      where: { id },
      data: { auto_delete_at }
    });
    
    // Log audit
    await prisma.auditLog.create({
      data: {
        userId: req.user.userId,
        caseId: id,
        action: autoDelete ? 'Scheduled for deletion (30 days)' : 'Retention policy removed (Permanent)',
      }
    });

    res.json(updated);
  } catch (err) {
    next(err);
  }
};

exports.deleteCase = async (req, res, next) => {
  try {
    const { id } = req.params;

    // Verify case exists before attempting deletion
    const existing = await prisma.case.findUnique({ where: { id } });
    if (!existing) return res.status(404).json({ error: 'Not found' });
    if (existing.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    // Delete child records first (foreign key constraints)
    await prisma.analysis.deleteMany({ where: { caseId: id } });
    await prisma.chainOfCustody.deleteMany({ where: { caseId: id } });
    await prisma.report.deleteMany({ where: { caseId: id } });
    await prisma.auditLog.deleteMany({ where: { caseId: id } });
    await prisma.case.delete({ where: { id } });

    res.json({ message: 'Case deleted successfully' });
  } catch (err) {
    next(err);
  }
};

exports.getCaseTimeline = async (req, res, next) => {
  try {
    const { id } = req.params;
    const c = await prisma.case.findUnique({ where: { id }, include: { analyses: true, auditLogs: true, reports: true } });
    if (!c) return res.status(404).json({ error: 'Not found' });
    if (c.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });
    // Flatten timeline items
    const timeline = [];
    (c.analyses || []).forEach(a => timeline.push({
      id: a.id,
      type: 'analysis',
      at: a.analyzed_at,
      title: 'Forensic Analysis Completed',
      description: `Threat Score: ${a.threat_score || 0} (${a.severity || 'Low'})`
    }));
    (c.reports || []).forEach(r => timeline.push({
      id: r.id,
      type: 'report',
      at: r.generated_at,
      title: `Report v${r.version} Generated`,
      description: `Digital signature: ${r.digital_signature ? 'Verified' : 'Pending'}`
    }));
    (c.auditLogs || []).forEach(l => timeline.push({
      id: l.id,
      type: 'audit',
      at: l.timestamp,
      title: l.action,
      description: null
    }));
    timeline.sort((x, y) => new Date(x.at) - new Date(y.at));
    res.json(timeline);
  } catch (err) {
    next(err);
  }
};
