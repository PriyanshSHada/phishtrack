const prisma = require('../prismaClient');

exports.getStats = async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const cases = await prisma.case.count({ where: { userId } });
    const analyses = await prisma.analysis.count({ where: { case: { userId } } });
    const reports = await prisma.report.count({ where: { case: { userId } } });
    const highRisk = await prisma.analysis.count({
      where: {
        case: { userId },
        threat_score: { gte: 70 }
      }
    });
    res.json({ highRisk, cases, analyses, reports });
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
    const { month, year } = req.query;

    let startDateStr;
    let endDateStr;

    if (month && year) {
      // Month is 1-12. Construct the 1st day of the month.
      const date = new Date(Date.UTC(parseInt(year), parseInt(month) - 1, 1));
      startDateStr = date.toISOString().split('T')[0];
      // Construct the last day of the month (day 0 of the next month)
      const endDate = new Date(Date.UTC(parseInt(year), parseInt(month), 0));
      endDateStr = endDate.toISOString().split('T')[0];
    } else {
      // Default: last 28 days
      const today = new Date();
      endDateStr = today.toISOString().split('T')[0];
      const startDate = new Date();
      startDate.setDate(today.getDate() - 27);
      startDateStr = startDate.toISOString().split('T')[0];
    }

    // 1. Get graph data for the given date range
    const currentWeekRaw = await prisma.$queryRaw`
      SELECT
        TO_CHAR(d.date, 'YYYY-MM-DD') as date,
        COALESCE(COUNT(c.id), 0)::int as count
      FROM generate_series(
        ${startDateStr}::date,
        ${endDateStr}::date,
        '1 day'::interval
      ) AS d(date)
      LEFT JOIN "Case" c ON DATE(c.created_at) = DATE(d.date) AND c."userId" = ${userId}
      GROUP BY d.date
      ORDER BY d.date ASC;
    `;

    // 2. Get totals for KPIs (We can adjust the totals to reflect the selected period or keep them as recent KPIs)
    // If we want the KPIs to be based on the selected period:
    const totalThisWeekRes = await prisma.$queryRaw`
      SELECT COUNT(*)::int as total
      FROM "Case"
      WHERE "userId" = ${userId} 
        AND DATE(created_at) >= ${startDateStr}::date 
        AND DATE(created_at) <= ${endDateStr}::date;
    `;
    const totalThisWeek = totalThisWeekRes[0].total;

    const totalLastWeekRes = await prisma.$queryRaw`
      SELECT COUNT(*)::int as total
      FROM "Case"
      WHERE "userId" = ${userId}
        AND DATE(created_at) >= (${startDateStr}::date - INTERVAL '7 days')
        AND DATE(created_at) < ${startDateStr}::date;
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
        case: {
          userId,
          status: {
            in: ['Open', 'Investigating']
          }
        }
      },
      orderBy: { analyzed_at: 'desc' },
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
