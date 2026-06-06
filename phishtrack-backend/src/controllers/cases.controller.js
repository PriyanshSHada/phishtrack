const prisma = require('../prismaClient');
const { generateCaseNumber } = require('../utils/caseNumber.util');

exports.getAllCases = async (req, res, next) => {
  try {
    const { status, priority, date } = req.query;
    const where = {};
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

    const cases = await prisma.case.findMany({ where, orderBy: { created_at: 'desc' } });
    res.json(cases);
  } catch (err) {
    next(err);
  }
};

exports.createCase = async (req, res, next) => {
  try {
    const { description, url, source, priority, tags } = req.body;
    if (!url) return res.status(400).json({ error: 'Missing url' });
    const startOfYear = new Date(new Date().getFullYear(), 0, 1);
    const seq = await prisma.case.count({ where: { created_at: { gte: startOfYear } } });
    const caseNumber = generateCaseNumber(seq + 1);
    
    let userId = req.user?.userId;
    if (!userId) {
      const firstUser = await prisma.user.findFirst();
      if (!firstUser) return res.status(400).json({ error: 'No user available to assign case' });
      userId = firstUser.id;
    }

    const data = {
      case_number: caseNumber,
      userId,
      url: url || '',
      description: description || '',
      source: source || 'Other',
      priority: priority || 'Low',
      tags: Array.isArray(tags) ? tags : []
    };
    const created = await prisma.case.create({ data });
    res.status(201).json(created);
  } catch (err) {
    next(err);
  }
};

exports.getCaseById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const c = await prisma.case.findUnique({ where: { id }, include: { analyses: true, reports: true, auditLogs: true } });
    if (!c) return res.status(404).json({ error: 'Not found' });
    res.json(c);
  } catch (err) {
    next(err);
  }
};

exports.updateCase = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status, priority, description } = req.body;

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

exports.deleteCase = async (req, res, next) => {
  try {
    const { id } = req.params;
    await prisma.case.delete({ where: { id } });
    res.status(204).end();
  } catch (err) {
    next(err);
  }
};

exports.getCaseTimeline = async (req, res, next) => {
  try {
    const { id } = req.params;
    const c = await prisma.case.findUnique({ where: { id }, include: { analyses: true, auditLogs: true, reports: true } });
    if (!c) return res.status(404).json({ error: 'Not found' });
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
