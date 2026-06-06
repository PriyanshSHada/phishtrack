const prisma = require('../prismaClient');
const { sign, verify } = require('../utils/signature.util');
const pdfService = require('../services/report/pdf.service');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

// Helper function to calculate SHA-256 of a file
function getFileHash(filePath) {
  return new Promise((resolve, reject) => {
    if (!fs.existsSync(filePath)) {
      return resolve(null);
    }
    const hash = crypto.createHash('sha256');
    const stream = fs.createReadStream(filePath);
    stream.on('data', (data) => hash.update(data));
    stream.on('end', () => resolve(hash.digest('hex')));
    stream.on('error', (err) => reject(err));
  });
}

exports.generateReport = async (req, res, next) => {
  try {
    const caseId = req.params.caseId || req.body.caseId;
    if (!caseId) {
      return res.status(400).json({ error: 'Missing caseId' });
    }

    // Resolve case, latest analysis, and user
    const caseData = await prisma.case.findUnique({ where: { id: caseId } });
    if (!caseData) {
      return res.status(404).json({ error: 'Case not found' });
    }

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

    // Find latest report version to increment
    const latestReport = await prisma.report.findFirst({
      where: { caseId },
      orderBy: { version: 'desc' }
    });
    const version = latestReport ? latestReport.version + 1 : 1;

    // Get previous report PDF hash if exists (for Chain of Custody)
    let hashBefore = null;
    if (latestReport) {
      const prevPath = path.join(__dirname, '../../uploads/reports', `${latestReport.id}.pdf`);
      if (fs.existsSync(prevPath)) {
        hashBefore = await getFileHash(prevPath);
      }
    }

    const generatedAt = new Date();
    const reportId = crypto.randomUUID();
    const reportFilename = `${reportId}.pdf`;
    
    // Ensure directory exists
    const reportsDir = path.join(__dirname, '../../uploads/reports');
    if (!fs.existsSync(reportsDir)) {
      fs.mkdirSync(reportsDir, { recursive: true });
    }
    const outputPath = path.join(reportsDir, reportFilename);

    // Compute HMAC signature over core metadata
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

    // Fetch chain of custody for the report
    const custodyChain = await prisma.chainOfCustody.findMany({
      where: { caseId },
      orderBy: { timestamp: 'asc' },
      include: {
        user: { select: { id: true, name: true, email: true } }
      }
    });

    // Generate PDF
    await pdfService.generatePdfReport(outputPath, {
      case: caseData,
      analysis: analysis,
      analyst: user,
      digitalSignature: digitalSignature,
      version: version,
      generated_at: generatedAt,
      custodyChain
    });

    // Compute file hash after creation
    const hashAfter = await getFileHash(outputPath);

    // Create Report in Database
    const report = await prisma.report.create({
      data: {
        id: reportId,
        caseId: caseData.id,
        version: version,
        pdf_url: `/api/reports/${reportId}/pdf`,
        digital_signature: digitalSignature,
        generatedById: user.id,
        generated_at: generatedAt
      }
    });

    // Write Chain of Custody entry
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

    // Add Audit Log
    await prisma.auditLog.create({
      data: {
        userId: user.id,
        caseId: caseData.id,
        action: 'REPORT_GENERATED',
        metadata: {
          reportId: report.id,
          version: version,
          digital_signature: digitalSignature
        },
        timestamp: generatedAt
      }
    });

    res.status(201).json(report);
  } catch (err) {
    console.error('Error generating report:', err);
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
    res.json(report);
  } catch (err) {
    next(err);
  }
};

exports.getReportByCase = async (req, res, next) => {
  try {
    const { caseId } = req.params;
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
    const report = await prisma.report.findUnique({ where: { id } });
    if (!report) {
      return res.status(404).json({ error: 'Report not found' });
    }
    const filePath = path.join(__dirname, '../../uploads/reports', `${id}.pdf`);

    // If file doesn't exist (Render ephemeral disk), regenerate it
    if (!fs.existsSync(filePath)) {
      const caseData = await prisma.case.findUnique({ where: { id: report.caseId } });
      const analysis = await prisma.analysis.findFirst({
        where: { caseId: report.caseId },
        orderBy: { analyzed_at: 'desc' }
      });
      const user = await prisma.user.findUnique({ where: { id: report.generatedById } });

      if (!caseData) {
        return res.status(404).json({ error: 'Associated case not found' });
      }

      // Ensure directory exists
      const reportsDir = path.join(__dirname, '../../uploads/reports');
      if (!fs.existsSync(reportsDir)) {
        fs.mkdirSync(reportsDir, { recursive: true });
      }

      await pdfService.generatePdfReport(filePath, {
        case: caseData,
        analysis: analysis || {},
        analyst: user || { name: 'Unknown', email: '' },
        digitalSignature: report.digital_signature || 'N/A',
        version: report.version,
        generated_at: report.generated_at
      });
    }

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="PhishTrack_Report_Case_${report.caseId}_v${report.version}.pdf"`);
    fs.createReadStream(filePath).pipe(res);
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

    // Fetch analysis at the time of report generation
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

    // 1. Verify HMAC Signature
    const isValidHmac = verify(payload, report.digital_signature);

    // 2. Verify File Hash in Chain of Custody
    const filePath = path.join(__dirname, '../../uploads/reports', `${id}.pdf`);
    const fileExists = fs.existsSync(filePath);
    
    let isHashValid = false;
    let currentFileHash = null;

    const custody = await prisma.chainOfCustody.findFirst({
      where: {
        caseId: report.caseId,
        action: `REPORT_GENERATED_V${report.version}`
      }
    });

    if (fileExists && custody) {
      currentFileHash = await getFileHash(filePath);
      isHashValid = (currentFileHash === custody.hash_after);
    }

    const verified = isValidHmac && fileExists && isHashValid;

    // If verification failed and report is not marked as tampered, update it
    if (!verified && !report.is_tampered) {
      await prisma.report.update({
        where: { id },
        data: { is_tampered: true }
      });
      
      // Log tampering to audit logs
      await prisma.auditLog.create({
        data: {
          userId: req.user.userId,
          caseId: report.caseId,
          action: 'REPORT_TAMPERED_DETECTED',
          metadata: {
            reportId: report.id,
            reason: !isValidHmac ? 'HMAC_MISMATCH' : (!fileExists ? 'FILE_MISSING' : 'HASH_MISMATCH')
          }
        }
      });
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
    console.error('Error verifying report:', err);
    next(err);
  }
};

