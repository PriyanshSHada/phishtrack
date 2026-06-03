const prisma = require('../prismaClient');

exports.getStats = async (req, res, next) => {
  try {
    const users = await prisma.user.count();
    const cases = await prisma.case.count();
    const analyses = await prisma.analysis.count();
    const reports = await prisma.report.count();
    res.json({ users, cases, analyses, reports });
  } catch (err) {
    next(err);
  }
};

exports.getRecentCases = async (req, res, next) => {
  try {
    const recent = await prisma.case.findMany({ orderBy: { createdAt: 'desc' }, take: 10 });
    res.json(recent);
  } catch (err) {
    next(err);
  }
};

exports.getWeeklyGraph = async (req, res, next) => {
  try {
    const now = new Date();
    const days = 7;
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (days - 1));
    const cases = await prisma.case.findMany({ where: { createdAt: { gte: start } } });
    const buckets = {};
    for (let i = 0; i < days; i++) {
      const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      const key = d.toISOString().slice(0, 10);
      buckets[key] = 0;
    }
    cases.forEach(c => {
      const key = new Date(c.createdAt).toISOString().slice(0, 10);
      if (buckets[key] !== undefined) buckets[key]++;
    });
    res.json(Object.entries(buckets).map(([date, count]) => ({ date, count })));
  } catch (err) {
    next(err);
  }
};

exports.getThreatMap = async (req, res, next) => {
  try {
    const analyses = await prisma.analysis.findMany({
      orderBy: { analyzed_at: 'desc' },
      take: 100
    });

    const locations = analyses
      .filter(a => a.ip_geolocation && !a.ip_geolocation.error && a.ip_geolocation.lat !== undefined && a.ip_geolocation.lon !== undefined)
      .map(a => ({
        ip: a.ip_geolocation.ip || null,
        country: a.ip_geolocation.country || null,
        city: a.ip_geolocation.city || null,
        latitude: a.ip_geolocation.lat,
        longitude: a.ip_geolocation.lon,
        threat_score: a.threat_score || 0
      }));

    res.json(locations);
  } catch (err) {
    next(err);
  }
};
