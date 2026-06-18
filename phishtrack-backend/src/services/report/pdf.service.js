const PDFDocument = require('pdfkit');
const fs = require('fs');

exports.generatePdfReport = (data) => {
  return new Promise((resolve, reject) => {
    try {
      const doc = new PDFDocument({ margin: 50, size: 'A4', bufferPages: true });
      const buffers = [];
      doc.on('data', buffers.push.bind(buffers));
      doc.on('end', () => {
        const pdfData = Buffer.concat(buffers);
        resolve(pdfData);
      });

      // ── Professional Color Palette ──
      const colors = {
        dark: '#0A0E1A',
        primary: '#141829',
        accent: '#00F5FF',
        danger: '#FF3B3B',
        warning: '#FFA500',
        success: '#00FF88',
        text: '#1A1A2E',
        subtext: '#555571',
        muted: '#8892B0',
        white: '#FFFFFF',
        lightBg: '#F0F2FA',
        border: '#D1D5DB',
      };

      const caseData = data.case || {};
      const analysis = data.analysis || {};
      const analyst = data.analyst || {};
      const score = analysis.threat_score || 0;
      const severityColor =
        score >= 70 ? colors.danger : score >= 40 ? colors.warning : colors.success;

      // Helper: draw a gradient rectangle
      function gradientRect(x, y, w, h) {
        for (let i = 0; i < h; i++) {
          const ratio = i / h;
          doc.opacity(0.02 + ratio * 0.06)
             .rect(x, y + i, w, 1)
             .fill(colors.accent);
        }
        doc.opacity(1);
      }

      // ── COVER PAGE ──
      // Background accent strips
      doc.rect(0, 0, 8, 842).fill(severityColor);
      doc.rect(0, 0, 595.28, 120).fill(colors.dark);

      // Shield icon (text-based for simplicity)
      doc.fillColor(colors.accent)
         .font('Helvetica-Bold')
         .fontSize(48)
         .text('🛡️', 50, 25);

      // Title block
      doc.fillColor(colors.white)
         .font('Helvetica-Bold')
         .fontSize(28)
         .text('PHISHTRACK', 50, 35);

      doc.fillColor(colors.muted)
         .font('Helvetica')
         .fontSize(12)
         .text('FORENSIC ANALYSIS REPORT', 50, 68);

      // Severity badge on cover
      doc.rect(410, 30, 140, 50).fill(severityColor).opacity(0.12);
      doc.opacity(1); // Reset opacity so subsequent content is not dimmed
      doc.rect(410, 30, 140, 50).stroke(severityColor).lineWidth(1.5);
      doc.fillColor(severityColor)
         .font('Helvetica-Bold')
         .fontSize(11)
         .text('THREAT SCORE', 420, 38);
      doc.fontSize(28)
         .text(`${score}/100`, 420, 52);

      // Case metadata box
      doc.rect(30, 180, 535, 160).fill(colors.lightBg);
      doc.rect(30, 180, 535, 160).stroke(colors.border).lineWidth(0.5);

      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(14)
         .text('CASE DETAILS', 50, 195);

      const leftX = 50;
      const rightX = 320;
      let y = 220;

      const metaRows = [
        ['Case Number:', caseData.case_number || 'N/A'],
        ['Target URL:', caseData.url || 'N/A'],
        ['Status:', caseData.status || 'N/A'],
        ['Priority:', caseData.priority || 'N/A'],
        ['Source:', caseData.source || 'N/A'],
        ['Analyst:', analyst.name || 'System Assigned'],
        ['Severity:', analysis.severity || 'Low'],
        ['Date:', new Date(caseData.created_at).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })],
      ];

      metaRows.forEach(([label, value], i) => {
        const col = i < 4 ? leftX : rightX;
        const rowY = y + (i % 4) * 30;
        doc.font('Helvetica-Bold').fontSize(9).fillColor(colors.subtext).text(label, col, rowY);
        doc.font('Helvetica').fontSize(10).fillColor(colors.text).text(String(value), col + 90, rowY);
      });

      // Threat gauge on cover
      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(11)
         .text('RISK ASSESSMENT', 50, 370);

      // Gauge bar
      const gaugeY = 392;
      doc.rect(50, gaugeY, 495, 18).fill('#E5E7EB').radius(4);
      const fillWidth = Math.min(495, (score / 100) * 495);
      doc.rect(50, gaugeY, fillWidth, 18).fill(severityColor).radius(4);

      doc.fillColor(colors.white)
         .font('Helvetica-Bold')
         .fontSize(10)
         .text(`${score}%`, 290, gaugeY + 2);

      // ── PAGE 2: AI ANALYSIS ──
      doc.addPage();
      doc.rect(0, 0, 595.28, 20).fill(colors.dark);

      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(16)
         .text('AI FORENSIC ANALYSIS', 50, 35);

      // AI Summary box
      const summaryText = analysis.ai_summary || 'No AI analysis available for this case.';
      doc.rect(45, 60, 505, 100).fill(colors.lightBg);
      doc.rect(45, 60, 505, 100).stroke(severityColor).lineWidth(1.5);

      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(10)
         .text('GPT-4o FORENSIC EVALUATION', 60, 70);

      doc.font('Helvetica')
         .fontSize(10)
         .fillColor(colors.text)
         .text(summaryText, 60, 88, { width: 475, align: 'justify', lineGap: 3 });

      // Threat Indicators
      let indicatorY = 175;
      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(14)
         .text('THREAT INDICATORS', 50, indicatorY);

      const indicators = analysis.ai_indicators || [];
      if (indicators.length === 0) {
        doc.font('Helvetica-Oblique').fontSize(10).fillColor(colors.subtext)
           .text('No specific indicators were flagged.', 50, indicatorY + 25);
      } else {
        indicatorY += 30;
        indicators.forEach((ind, i) => {
          const badgeY = indicatorY + Math.floor(i / 2) * 24;
          const badgeX = 50 + (i % 2) * 270;
          doc.rect(badgeX, badgeY, 250, 20).fill(colors.danger).opacity(0.08);
          doc.rect(badgeX, badgeY, 250, 20).stroke(colors.danger).lineWidth(0.5).opacity(0.4);
          doc.fillColor(colors.danger)
             .font('Helvetica-Bold')
             .fontSize(8)
             .text(`⚠ ${ind}`, badgeX + 8, badgeY + 5, { width: 234 });
          doc.opacity(1);
        });
      }

      // Techniques
      const techniques = analysis.ai_techniques || [];
      let techY = indicators.length > 0 ? indicatorY + Math.ceil(indicators.length / 2) * 24 + 20 : indicatorY + 40;

      if (techniques.length > 0) {
        doc.fillColor(colors.dark)
           .font('Helvetica-Bold')
           .fontSize(14)
           .text('DETECTED TECHNIQUES', 50, techY);

        techY += 25;
        techniques.forEach((tech, i) => {
          doc.rect(50 + (i % 3) * 170, techY + Math.floor(i / 3) * 22, 155, 18)
             .fill(colors.warning).opacity(0.08);
          doc.fillColor(colors.warning)
             .font('Helvetica')
             .fontSize(8)
             .text(`• ${tech}`, 58 + (i % 3) * 170, techY + Math.floor(i / 3) * 22 + 4, { width: 145 });
          doc.opacity(1);
        });
        techY += Math.ceil(techniques.length / 3) * 22 + 20;
      }

      // ── PAGE 3: FORENSIC ARTIFACTS ──
      doc.addPage();
      doc.rect(0, 0, 595.28, 20).fill(colors.dark);

      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(16)
         .text('FORENSIC ARTIFACT ANALYSIS', 50, 35);

      // Section helper
      function section(title, yStart) {
        doc.rect(45, yStart - 5, 505, 1).fill(colors.accent).opacity(0.3);
        doc.opacity(1);
        doc.fillColor(colors.dark)
           .font('Helvetica-Bold')
           .fontSize(12)
           .text(title, 50, yStart);
        return yStart + 20;
      }

      let artY = 55;

      // WHOIS Section
      artY = section('WHOIS DOMAIN REGISTRY', artY);
      const whois = analysis.whois_data || {};
      const whoisData = [
        ['Registrar', whois.registrar || 'Unknown'],
        ['Country', whois.country || 'Unknown'],
        ['Domain Age', whois.ageDays != null ? `${whois.ageDays} days` : 'Unknown'],
        ['Creation Date', whois.creationDate ? new Date(whois.creationDate).toLocaleDateString() : 'Unknown'],
        ['Expiry Date', whois.expiryDate ? new Date(whois.expiryDate).toLocaleDateString() : 'Unknown'],
        ['Suspicious Age', whois.isSuspiciousAge ? 'YES ⚠' : 'No'],
      ];
      doc.font('Helvetica').fontSize(9);
      whoisData.forEach(([label, value], i) => {
        const rowX = 50 + (i % 2) * 270;
        const rowY = artY + Math.floor(i / 2) * 16;
        doc.fillColor(colors.subtext).text(label + ':', rowX, rowY);
        doc.fillColor(colors.text).text(String(value), rowX + 90, rowY);
      });
      artY += Math.ceil(whoisData.length / 2) * 16 + 15;

      // Network & SSL
      artY = section('NETWORK & SSL DETAILS', artY);
      const geo = analysis.ip_geolocation || {};
      const ssl = analysis.ssl_info || {};
      const netData = [
        ['Resolved IP', geo.ip || 'Unknown'],
        ['Location', `${geo.city || '?'}, ${geo.country || '?'}`],
        ['ISP', geo.isp || 'Unknown'],
        ['SSL Valid', ssl.valid === true ? 'Yes ✅' : 'No ⚠'],
        ['SSL Issuer', ssl.issuer || 'Unknown'],
      ];
      netData.forEach(([label, value], i) => {
        const rowX = 50 + (i % 2) * 270;
        const rowY = artY + Math.floor(i / 2) * 16;
        doc.fillColor(colors.subtext).text(label + ':', rowX, rowY);
        doc.fillColor(colors.text).text(String(value), rowX + 90, rowY);
      });
      artY += Math.ceil(netData.length / 2) * 16 + 15;

      // VirusTotal
      artY = section('VIRUSTOTAL SCAN RESULTS', artY);
      const vt = analysis.virustotal_result || {};
      doc.font('Helvetica').fontSize(10).fillColor(colors.text)
         .text(`Malicious: ${vt.maliciousCount || 0}  |  Suspicious: ${vt.suspiciousCount || 0}  |  Harmless: ${vt.harmlessCount || 0}`, 50, artY);
      artY += 25;

      // Detections table
      const detections = vt.detections || [];
      if (detections.length > 0) {
        doc.font('Helvetica-Bold').fontSize(9).fillColor(colors.subtext)
           .text('Engine', 55, artY)
           .text('Result', 250, artY);
        doc.moveTo(50, artY + 12).lineTo(545, artY + 12).stroke(colors.border).lineWidth(0.5);
        artY += 16;
        detections.slice(0, 8).forEach(d => {
          doc.font('Helvetica').fontSize(8)
             .fillColor(colors.text).text(d.engine, 55, artY)
             .fillColor(colors.danger).text(String(d.result || ''), 250, artY, { width: 280 });
          artY += 14;
        });
        artY += 10;
      }

      // Redirect Chain
      artY = section('REDIRECT CHAIN', Math.max(artY, 340));
      const chain = analysis.redirect_chain || [caseData.url];
      chain.forEach((url, i) => {
        doc.font('Helvetica').fontSize(9)
           .fillColor(i === chain.length - 1 ? colors.success : colors.accent)
           .text(`${i + 1}. ${url}`, 55, artY, { width: 480 });
        artY += 16;
      });
      artY += 20;

      // Screenshot
      artY = section('EVIDENCE SCREENSHOT', artY);
      const screenshot = analysis.page_screenshot;
      if (screenshot && screenshot.startsWith('data:image/png;base64,')) {
        try {
          const base64Data = screenshot.replace(/^data:image\/\w+;base64,/, '');
          const buffer = Buffer.from(base64Data, 'base64');
          // Guard against negative height when artY has grown large
          const availableHeight = Math.max(50, Math.min(350, 800 - artY - 50));
          if (artY + availableHeight > 800) {
            // Not enough space — add a new page for the screenshot
            doc.addPage();
            artY = 50;
          }
          doc.image(buffer, 50, artY, {
            fit: [495, availableHeight],
            align: 'center',
            valign: 'center',
          });
        } catch (imgErr) {
          doc.font('Helvetica-Oblique').fontSize(9).fillColor(colors.subtext)
             .text('Screenshot could not be rendered in PDF.', 50, artY);
        }
      } else {
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(colors.subtext)
           .text('No screenshot was captured during analysis.', 50, artY);
      }

      // ── PAGE 4: CHAIN OF CUSTODY & LEGAL ──
      doc.addPage();
      doc.rect(0, 0, 595.28, 20).fill(colors.dark);

      doc.fillColor(colors.dark)
         .font('Helvetica-Bold')
         .fontSize(16)
         .text('CHAIN OF CUSTODY', 50, 35);

      let custodyY = 60;

      const custodyChain = data.custodyChain || [];
      if (custodyChain.length > 0) {
        // Table header
        doc.font('Helvetica-Bold').fontSize(8).fillColor(colors.subtext);
        doc.text('Timestamp', 50, custodyY);
        doc.text('Action', 180, custodyY);
        doc.text('Analyst', 310, custodyY);
        doc.text('Hash (SHA-256)', 400, custodyY);
        doc.moveTo(50, custodyY + 12).lineTo(545, custodyY + 12).stroke(colors.border).lineWidth(0.5);
        custodyY += 16;

        custodyChain.forEach((entry, i) => {
          if (i > 0) {
            doc.moveTo(50, custodyY - 2).lineTo(545, custodyY - 2).stroke(colors.border).lineWidth(0.3).opacity(0.3);
            doc.opacity(1);
          }
          const ts = new Date(entry.timestamp).toLocaleString('en-US', { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' });
          const hashShort = (entry.hash_after || 'N/A').substring(0, 12) + '...';
          const analystName = entry.user?.name || entry.userId || 'System';

          doc.font('Helvetica').fontSize(7)
             .fillColor(colors.muted).text(ts, 50, custodyY, { width: 120 })
             .fillColor(colors.text).text(entry.action, 180, custodyY, { width: 120 })
             .fillColor(colors.subtext).text(analystName, 310, custodyY, { width: 80 })
             .font('Courier').fontSize(6).fillColor(colors.accent)
             .text(hashShort, 400, custodyY, { width: 140 });

          custodyY += 16;
        });
        custodyY += 10;
      } else {
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(colors.subtext)
           .text('No chain of custody records have been created yet. Records are generated when reports are compiled and cases are analyzed.', 50, custodyY, { width: 495, lineGap: 2 });
        custodyY += 40;
      }

      doc.font('Helvetica').fontSize(8).fillColor(colors.subtext)
         .text('The above cryptographic hashes establish the forensic integrity of this report. Each entry represents a verifiable event in the case lifecycle.', 50, custodyY, { width: 495 });
      custodyY += 20;

      // Footer on every page
      const range = doc.bufferedPageRange();
      for (let i = 0; i < range.count; i++) {
        doc.switchToPage(i);
        doc.fillColor(colors.muted)
           .font('Helvetica')
           .fontSize(7)
           .text(
             `PhishTrack Forensic Report | Generated: ${new Date(data.generated_at).toUTCString()} | Page ${i + 1}/${range.count}`,
             50, 810, { width: 495, align: 'center' }
           );
      }

      // Final page: digital signature + legal notice
      doc.switchToPage(range.count - 1);
      const footerY = 700;

      doc.moveTo(50, footerY).lineTo(545, footerY).stroke(colors.border).lineWidth(1);

      doc.fillColor(colors.subtext)
         .font('Helvetica-Bold')
         .fontSize(8)
         .text('DIGITAL FORENSIC SIGNATURE (HMAC-SHA256)', 50, footerY + 15);

      doc.font('Courier')
         .fontSize(7)
         .fillColor(colors.text)
         .text(data.digitalSignature || 'N/A', 50, footerY + 27, { width: 495, wordBreak: true });

      doc.font('Helvetica')
         .fontSize(8)
         .fillColor(colors.subtext)
         .text(`Report Version: ${data.version || 1}  |  Analyst: ${analyst.name || 'System'}  |  ${new Date(data.generated_at).toUTCString()}`, 50, footerY + 50);

      doc.text(
        'This report was automatically generated by PhishTrack Forensics. The digital signature above cryptographically binds the case metadata, threat assessment, and forensic analysis data. Any alteration to this report will invalidate the HMAC-SHA256 signature. This document serves as a legally admissible forensic artifact in cybersecurity investigations.',
        50, footerY + 70, { width: 495, align: 'justify', lineGap: 2 }
      );

      doc.end();

    } catch (err) {
      reject(err);
    }
  });
};