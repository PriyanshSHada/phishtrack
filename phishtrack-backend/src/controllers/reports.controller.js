const prisma = require('../prismaClient');
const { sign, verify } = require('../utils/signature.util');
const pdfService = require('../services/report/pdf.service');
const logger = require('../utils/logger');
const crypto = require('crypto');
const storageService = require('../services/storage.service');

exports.generateReport = async (req, res, next) => {
  try {
    const caseId = req.params.caseId || req.body.caseId;
    if (!caseId) {
      return res.status(400).json({ error: 'Missing caseId' });
    }

    const caseData = await prisma.case.findUnique({ where: { id: caseId } });
    if (!caseData) {
      return res.status(404).json({ error: 'Case not found' });
    }
    if (caseData.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    const analysis = await prisma.analysis.findFirst({
      where: { caseId },
      orderBy: { analyzed_at: 'desc' }
    });
    if (!analysis) {
      return res.status(400).json({ error: 'No analysis found for this case. Reports require threat assessment data.' });
    }

    const userId = req.user.userId;
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    const latestReport = await prisma.report.findFirst({
      where: { caseId },
      orderBy: { version: 'desc' }
    });
    const version = latestReport ? latestReport.version + 1 : 1;

    let hashBefore = null;
    if (latestReport) {
      const prevCustody = await prisma.chainOfCustody.findFirst({
        where: { caseId, action: `REPORT_GENERATED_V${latestReport.version}` }
      });
      if (prevCustody) {
        hashBefore = prevCustody.hash_after;
      }
    }

    const generatedAt = new Date();
    const reportId = crypto.randomUUID();

    const payload = {
      caseId: caseData.id,
      caseNumber: caseData.case_number,
      threatScore: analysis.threat_score || 0,
      severity: analysis.severity || 'Low',
      version: version,
      generatedById: user.id,
      generatedAt: generatedAt.toISOString()
    };
    const digitalSignature = sign(payload);

    const custodyChain = await prisma.chainOfCustody.findMany({
      where: { caseId },
      orderBy: { timestamp: 'asc' },
      include: {
        user: { select: { id: true, name: true, email: true } }
      }
    });

    const pdfBuffer = await pdfService.generatePdfReport({
      case: caseData,
      analysis: analysis,
      analyst: user,
      digitalSignature: digitalSignature,
      version: version,
      generated_at: generatedAt,
      custodyChain
    });

    const hashAfter = crypto.createHash('sha256').update(pdfBuffer).digest('hex');

    const supabasePath = await storageService.uploadReportPdf(reportId, pdfBuffer);

    const report = await prisma.report.create({
      data: {
        id: reportId,
        caseId: caseData.id,
        version: version,
        pdf_url: `/api/reports/${reportId}/pdf`,
        supabase_path: supabasePath,
        digital_signature: digitalSignature,
        generatedById: user.id,
        generated_at: generatedAt
      }
    });

    await prisma.chainOfCustody.create({
      data: {
        caseId: caseData.id,
        userId: user.id,
        action: `REPORT_GENERATED_V${version}`,
        hash_before: hashBefore,
        hash_after: hashAfter,
        timestamp: generatedAt
      }
    });

    await prisma.auditLog.create({
      data: {
        userId: user.id,
        caseId: caseData.id,
        action: 'REPORT_GENERATED',
        metadata: {
          reportId: report.id,
          version: version,
          digital_signature: digitalSignature,
          supabase_path: supabasePath
        },
        timestamp: generatedAt
      }
    });

    res.status(201).json(report);
  } catch (err) {
    logger.error('Error generating report', { error: err.message, stack: err.stack, caseId: req.params.caseId });
    next(err);
  }
};

exports.getReport = async (req, res, next) => {
  try {
    const { id } = req.params;
    const report = await prisma.report.findUnique({
      where: { id },
      include: {
        case: true,
        generated_by: {
          select: {
            id: true,
            name: true,
            email: true
          }
        }
      }
    });
    if (!report) return res.status(404).json({ error: 'Report not found' });
    if (report.case.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });
    res.json(report);
  } catch (err) {
    next(err);
  }
};

exports.getReportByCase = async (req, res, next) => {
  try {
    const { caseId } = req.params;
    
    // Verify case ownership
    const caseData = await prisma.case.findUnique({ where: { id: caseId } });
    if (!caseData) return res.status(404).json({ error: 'Case not found' });
    if (caseData.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    const reports = await prisma.report.findMany({
      where: { caseId },
      orderBy: { version: 'desc' }
    });
    res.json(reports);
  } catch (err) {
    next(err);
  }
};

exports.downloadPdf = async (req, res, next) => {
  try {
    const { id } = req.params;
    const report = await prisma.report.findUnique({ where: { id }, include: { case: true } });
    if (!report) {
      return res.status(404).json({ error: 'Report not found' });
    }
    if (report.case.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    if (report.supabase_path) {
      const fileName = report.supabase_path.split('/').pop();
      const signedUrl = await storageService.getSignedUrl(fileName);
      if (signedUrl) {
        return res.redirect(302, signedUrl);
      }
    }

    return res.status(404).json({ error: 'PDF not available in cloud storage' });
  } catch (err) {
    next(err);
  }
};

exports.verifyReport = async (req, res, next) => {
  try {
    const { id } = req.params;
    const report = await prisma.report.findUnique({
      where: { id },
      include: {
        case: true
      }
    });
    if (!report) return res.status(404).json({ error: 'Report not found' });
    if (report.case.userId !== req.user.userId) return res.status(403).json({ error: 'Access denied' });

    const analysis = await prisma.analysis.findFirst({
      where: { caseId: report.caseId },
      orderBy: { analyzed_at: 'desc' }
    });

    const generatedAtStr = new Date(report.generated_at).toISOString();
    const payload = {
      caseId: report.caseId,
      caseNumber: report.case.case_number,
      threatScore: analysis?.threat_score || 0,
      severity: analysis?.severity || 'Low',
      version: report.version,
      generatedById: report.generatedById,
      generatedAt: generatedAtStr
    };

    const isValidHmac = verify(payload, report.digital_signature);

    let isHashValid = false;
    let currentFileHash = null;
    let fileExists = false;

    if (report.supabase_path) {
      const fileName = report.supabase_path.split('/').pop();
      const cloudBuffer = await storageService.downloadReportPdf(fileName);
      if (cloudBuffer) {
        fileExists = true;
        currentFileHash = crypto.createHash('sha256').update(cloudBuffer).digest('hex');
      }
    }

    const custody = await prisma.chainOfCustody.findFirst({
      where: {
        caseId: report.caseId,
        action: `REPORT_GENERATED_V${report.version}`
      }
    });

    if (fileExists && custody) {
      isHashValid = (currentFileHash === custody.hash_after);
    }

    const verified = isValidHmac && fileExists && isHashValid;

    if (!verified && !report.is_tampered) {
      await prisma.report.update({
        where: { id },
        data: { is_tampered: true }
      });
      
      const auditUserId = req.user?.userId || report.generatedById;
      if (auditUserId) {
        await prisma.auditLog.create({
          data: {
            userId: auditUserId,
            caseId: report.caseId,
            action: 'REPORT_TAMPERED_DETECTED',
            metadata: {
              reportId: report.id,
              reason: !isValidHmac ? 'HMAC_MISMATCH' : (!fileExists ? 'FILE_MISSING' : 'HASH_MISMATCH')
            }
          }
        });
      }
    }

    res.json({
      valid: verified,
      details: {
        hmac_valid: isValidHmac,
        file_exists: fileExists,
        file_hash_valid: isHashValid,
        stored_hash: custody?.hash_after || null,
        computed_hash: currentFileHash
      }
    });
  } catch (err) {
    logger.error('Error verifying report', { error: err.message, stack: err.stack });
    next(err);
  }
};
