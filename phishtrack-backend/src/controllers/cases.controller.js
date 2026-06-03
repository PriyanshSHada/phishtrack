const prisma = require('../prismaClient');
const { generateCaseNumber } = require('../utils/caseNumber.util');

exports.getAllCases = async (req, res, next) => {
  try {
    const cases = await prisma.case.findMany({ orderBy: { created_at: 'desc' } });
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
    const c = await prisma.case.findUnique({ where: { id }, include: { analyses: true, reports: true } });
    if (!c) return res.status(404).json({ error: 'Not found' });
    res.json(c);
  } catch (err) {
    next(err);
  }
};

exports.updateCase = async (req, res, next) => {
  try {
    const { id } = req.params;
    const updated = await prisma.case.update({ where: { id }, data: req.body });
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
    (c.analyses || []).forEach(a => timeline.push({ type: 'analysis', at: a.createdAt, data: a }));
    (c.reports || []).forEach(r => timeline.push({ type: 'report', at: r.createdAt, data: r }));
    (c.auditLogs || []).forEach(l => timeline.push({ type: 'audit', at: l.createdAt, data: l }));
    timeline.sort((x, y) => new Date(x.at) - new Date(y.at));
    res.json({ case: c, timeline });
  } catch (err) {
    next(err);
  }
};
