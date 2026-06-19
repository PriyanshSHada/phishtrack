const prisma = require('../prismaClient');

exports.getStats = async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const users = 1; // Logged-in user represents 1 user
    const cases = await prisma.case.count({ where: { userId } });
    const analyses = await prisma.analysis.count({ where: { case: { userId } } });
    const reports = await prisma.report.count({ where: { case: { userId } } });
    res.json({ users, cases, analyses, reports });
  } catch (err) {
    next(err);
  }
};

exports.getRecentCases = async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const recent = await prisma.case.findMany({ 
      where: { userId },
      orderBy: { created_at: 'desc' }, 
      take: 10 
    });
    res.json(recent);
  } catch (err) {
    next(err);
  }
};

exports.getWeeklyGraph = async (req, res, next) => {
  try {
    const userId = req.user.userId;

    // 1. Get current week data (last 7 days including today)
    const currentWeekRaw = await prisma.$queryRaw`
      SELECT
        TO_CHAR(d.date, 'YYYY-MM-DD') as date,
        COALESCE(COUNT(c.id), 0)::int as count
      FROM generate_series(
        CURRENT_DATE - INTERVAL '27 days',
        CURRENT_DATE,
        '1 day'::interval
      ) AS d(date)
      LEFT JOIN "Case" c ON DATE(c.created_at) = DATE(d.date) AND c."userId" = ${userId}
      GROUP BY d.date
      ORDER BY d.date ASC;
    `;

    // 2. Get totals for KPIs
    const totalThisWeekRes = await prisma.$queryRaw`
      SELECT COUNT(*)::int as total
      FROM "Case"
      WHERE "userId" = ${userId} AND created_at >= CURRENT_DATE - INTERVAL '7 days';
    `;
    const totalThisWeek = totalThisWeekRes[0].total;

    const totalLastWeekRes = await prisma.$queryRaw`
      SELECT COUNT(*)::int as total
      FROM "Case"
      WHERE "userId" = ${userId}
        AND created_at >= CURRENT_DATE - INTERVAL '14 days'
        AND created_at < CURRENT_DATE - INTERVAL '7 days';
    `;
    const totalLastWeek = totalLastWeekRes[0].total;

    res.json({
      currentWeek: currentWeekRaw,
      totalThisWeek,
      totalLastWeek
    });
  } catch (err) {
    next(err);
  }
};

exports.getThreatMap = async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const analyses = await prisma.analysis.findMany({
      where: {
        case: { userId }
      },
      orderBy: { analyzed_at: 'desc' },
      take: 100,
      include: {
        case: {
          select: {
            id: true,
            case_number: true,
            url: true,
            target_ip: true,
            priority: true
          }
        }
      }
    });

    const locations = analyses
      .filter(a => a.ip_geolocation && !a.ip_geolocation.error && a.ip_geolocation.lat !== undefined && a.ip_geolocation.lon !== undefined)
      .map(a => ({
        ip: a.ip_geolocation.ip || null,
        country: a.ip_geolocation.country || null,
        city: a.ip_geolocation.city || null,
        latitude: a.ip_geolocation.lat,
        longitude: a.ip_geolocation.lon,
        threat_score: a.threat_score || 0,
        severity: a.severity || 'Low',
        caseId: a.case.id,
        case_number: a.case.case_number,
        url: a.case.url || a.case.target_ip,
        priority: a.case.priority,
        ai_summary: a.ai_summary || null,
        ai_indicators: a.ai_indicators || [],
        isp: a.ip_geolocation.isp || null
      }));

    res.json(locations);
  } catch (err) {
    next(err);
  }
};
