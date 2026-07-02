const PDFDocument = require('pdfkit');

exports.generatePdfReport = (data) => {
  return new Promise((resolve, reject) => {
    try {
      const doc = new PDFDocument({ margin: 50, size: 'A4', bufferPages: true });
      const buffers = [];
      doc.on('data', buffers.push.bind(buffers));
      doc.on('end', () => resolve(Buffer.concat(buffers)));

      // ── Color Palette (dark, professional forensics theme) ──
      const c = {
        dark:     '#0A0E1A',
        navy:     '#141829',
        card:     '#1A2035',
        accent:   '#00F5FF',
        danger:   '#FF3B3B',
        warning:  '#FFA500',
        success:  '#00CC66',
        purple:   '#7C3AED',
        white:    '#FFFFFF',
        offwhite: '#E8EAF0',
        muted:    '#8892B0',
        subtle:   '#4A5270',
        border:   '#2A3558',
        lightBg:  '#F4F6FA',
        textDark: '#1E2235',
      };

      const caseData = data.case || {};
      const analysis = data.analysis || {};
      const analyst = data.analyst || {};
      const score = analysis.threat_score || 0;
      const confidence = analysis.confidence || 50;
      const verdict = analysis.verdict || 'Suspicious';
      const brandImpersonated = analysis.brand_impersonated || null;
      const whoisData = analysis.whois_data || analysis.whois || {};
      const ipGeoData = analysis.ip_geolocation || analysis.ipGeo || {};
      const sslData = analysis.ssl_info || analysis.ssl || {};
      const redirectChain = analysis.redirect_chain || analysis.redirectChain || [caseData.url || caseData.target_ip || 'N/A'];
      const screenshotData = analysis.page_screenshot || analysis.screenshot || null;
      const indicators = analysis.ai_indicators || analysis.indicators || [];
      const techniques = analysis.ai_techniques || analysis.techniques || [];
      const virustotalData = analysis.virustotal_result || analysis.virustotal || {};
      const mitreTechniques = (() => {
        try {
          if (Array.isArray(analysis.mitre_techniques)) return analysis.mitre_techniques;
          if (analysis.mitre_techniques && typeof analysis.mitre_techniques === 'object') {
            return Object.values(analysis.mitre_techniques);
          }
        } catch (_) {}
        return [];
      })();

      const severityColor = score >= 70 ? c.danger : score >= 40 ? c.warning : c.success;
      const verdictColor = {
        'Benign': c.success,
        'Suspicious': c.warning,
        'Likely Phishing': c.warning,
        'Confirmed Phishing': c.danger,
        'Malware Distribution': c.danger,
        'Credential Harvesting': c.danger
      }[verdict] || c.warning;

      const W = 595.28; // A4 width in points
      const MARGIN = 45;
      const CONTENT_W = W - MARGIN * 2;

      // ─── Helper functions ───────────────────────────────────────
      function drawPageHeader(pageTitle) {
        doc.rect(0, 0, W, 28).fill(c.dark);
        doc.fillColor(c.accent).font('Helvetica-Bold').fontSize(8)
           .text('PHISHTRACK', MARGIN, 9);
        doc.fillColor(c.muted).font('Helvetica').fontSize(7)
           .text(`${pageTitle}  |  CONFIDENTIAL FORENSIC DOCUMENT`, MARGIN + 80, 10);
        doc.fillColor(c.subtle).font('Courier').fontSize(6)
           .text(`CASE: ${caseData.case_number || 'N/A'}`, W - MARGIN - 110, 10);
      }

      function sectionLabel(text, y) {
        doc.rect(MARGIN, y, 3, 14).fill(c.accent);
        doc.fillColor(c.textDark).font('Helvetica-Bold').fontSize(11)
           .text(text, MARGIN + 10, y + 1);
        return y + 22;
      }

      function infoRow(label, value, x, y, width = 200) {
        doc.font('Helvetica-Bold').fontSize(8).fillColor(c.subtle).text(label, x, y);
        doc.font('Helvetica').fontSize(9).fillColor(c.textDark).text(String(value || 'N/A'), x, y + 11, { width });
        return y + 28;
      }

      function pillBadge(text, x, y, bgColor, textColor) {
        const pad = 8;
        const pillW = Math.min(text.length * 5.5 + pad * 2, 160);
        doc.rect(x, y, pillW, 14).fill(bgColor);
        doc.fillColor(textColor || '#FFFFFF').font('Helvetica-Bold').fontSize(7)
           .text(text.toUpperCase(), x + pad, y + 3, { width: pillW - pad * 2, align: 'center' });
        return x + pillW + 6;
      }

      function drawTableHeader(headers, colWidths, x, y) {
        doc.rect(x, y, colWidths.reduce((a, b) => a + b, 0), 16).fill(c.navy);
        let cx = x;
        headers.forEach((h, i) => {
          doc.fillColor(c.accent).font('Helvetica-Bold').fontSize(7)
             .text(h.toUpperCase(), cx + 4, y + 4, { width: colWidths[i] - 8 });
          cx += colWidths[i];
        });
        return y + 16;
      }

      function drawTableRow(cells, colWidths, x, y, rowIndex, cellColors) {
        const rowH = 14;
        const bgColor = rowIndex % 2 === 0 ? '#FFFFFF' : '#F7F8FB';
        doc.rect(x, y, colWidths.reduce((a, b) => a + b, 0), rowH).fill(bgColor);
        let cx = x;
        cells.forEach((cell, i) => {
          doc.fillColor(cellColors?.[i] || c.textDark).font('Helvetica').fontSize(7.5)
             .text(String(cell || 'N/A'), cx + 4, y + 3, { width: colWidths[i] - 8, ellipsis: true });
          cx += colWidths[i];
        });
        return y + rowH;
      }

      // ══════════════════════════════════════════════════════════
      // PAGE 1: COVER
      // ══════════════════════════════════════════════════════════
      // Full dark header banner
      doc.rect(0, 0, W, 130).fill(c.dark);
      // Severity accent strip on left
      doc.rect(0, 0, 6, 842).fill(severityColor);

      // Logo + title
      doc.fillColor(c.white).font('Helvetica-Bold').fontSize(26)
         .text('PHISHTRACK', MARGIN + 10, 28);
      doc.fillColor(c.accent).font('Helvetica').fontSize(10).letterSpacing = 2;
      doc.fillColor(c.accent).font('Helvetica').fontSize(10)
         .text('FORENSIC CYBER INTELLIGENCE REPORT', MARGIN + 10, 58);
      doc.fillColor(c.muted).font('Helvetica').fontSize(8)
         .text('DIGITAL FORENSICS  |  THREAT ANALYSIS  |  CHAIN OF CUSTODY', MARGIN + 10, 75);

      // Threat score badge (top right)
      const badgeX = W - 160;
      doc.rect(badgeX, 20, 115, 90).fill(severityColor + '1A'); // 10% opacity
      doc.rect(badgeX, 20, 115, 90).stroke(severityColor).lineWidth(1.5);
      doc.fillColor(c.muted).font('Helvetica-Bold').fontSize(8).text('THREAT SCORE', badgeX + 10, 30);
      doc.fillColor(severityColor).font('Helvetica-Bold').fontSize(40).text(`${score}`, badgeX + 12, 42);
      doc.fillColor(c.muted).font('Helvetica').fontSize(8).text('/ 100', badgeX + 62, 68);
      doc.fillColor(verdictColor).font('Helvetica-Bold').fontSize(9).text(verdict.toUpperCase(), badgeX + 10, 90);

      // Case info block
      const infoY = 145;
      doc.rect(MARGIN, infoY, CONTENT_W, 175).fill(c.lightBg);
      doc.rect(MARGIN, infoY, CONTENT_W, 175).stroke('#D5D9E8').lineWidth(0.5);

      doc.fillColor(c.textDark).font('Helvetica-Bold').fontSize(11).text('CASE OVERVIEW', MARGIN + 15, infoY + 12);
      doc.rect(MARGIN + 15, infoY + 26, 80, 0.5).fill(c.accent);

      const col1 = MARGIN + 15;
      const col2 = MARGIN + 260;
      let rowY = infoY + 35;

      const leftMeta = [
        ['Case Number', caseData.case_number || 'N/A'],
        ['Case Title', caseData.title || 'Untitled Case'],
        ['Target', caseData.url || caseData.target_ip || 'N/A'],
        ['Target Type', caseData.target_type || 'URL'],
      ];
      const rightMeta = [
        ['Priority', caseData.priority || 'N/A'],
        ['Source', caseData.source || 'N/A'],
        ['Status', caseData.status || 'N/A'],
        ['Analyst', analyst.name || 'System'],
      ];

      leftMeta.forEach(([label, value], i) => {
        doc.font('Helvetica-Bold').fontSize(8).fillColor(c.subtle).text(label + ':', col1, rowY + i * 32);
        doc.font('Helvetica').fontSize(9).fillColor(c.textDark).text(String(value), col1, rowY + i * 32 + 11, { width: 220, ellipsis: true });
      });
      rightMeta.forEach(([label, value], i) => {
        doc.font('Helvetica-Bold').fontSize(8).fillColor(c.subtle).text(label + ':', col2, rowY + i * 32);
        doc.font('Helvetica').fontSize(9).fillColor(c.textDark).text(String(value), col2, rowY + i * 32 + 11, { width: 200 });
      });

      // Report metadata row
      const metaY = infoY + 155;
      doc.font('Helvetica').fontSize(7.5).fillColor(c.subtle)
         .text(`Report v${data.version || 1}  |  Generated: ${new Date(data.generated_at).toUTCString()}  |  Analyst: ${analyst.email || 'N/A'}`, MARGIN + 15, metaY);

      // Threat assessment bar
      const barY = infoY + 205;
      doc.fillColor(c.textDark).font('Helvetica-Bold').fontSize(10).text('RISK ASSESSMENT BAR', MARGIN, barY);
      doc.roundedRect(MARGIN, barY + 18, CONTENT_W, 20, 4).fill('#E0E4EE');
      doc.roundedRect(MARGIN, barY + 18, Math.max(0, (score / 100) * CONTENT_W), 20, 4).fill(severityColor);
      doc.fillColor('#FFFFFF').font('Helvetica-Bold').fontSize(9)
         .text(`${score}% THREAT CONFIDENCE`, MARGIN + 10, barY + 22);

      // Severity + Verdict + Brand + Confidence chips
      const chipY = barY + 48;
      doc.fillColor(c.textDark).font('Helvetica-Bold').fontSize(9).text('CLASSIFICATION:', MARGIN, chipY);
      let chipX = MARGIN + 100;
      chipX = pillBadge(`Severity: ${analysis.severity || 'Low'}`, chipX, chipY - 1, severityColor, '#FFFFFF');
      chipX = pillBadge(`Verdict: ${verdict}`, chipX, chipY - 1, verdictColor, '#FFFFFF');
      chipX = pillBadge(`AI Confidence: ${confidence}%`, chipX, chipY - 1, c.purple, '#FFFFFF');
      if (brandImpersonated) {
        pillBadge(`Brand: ${brandImpersonated}`, chipX, chipY - 1, c.dark, c.accent);
      }

      // Created date
      doc.fillColor(c.subtle).font('Helvetica').fontSize(8)
         .text(`Case Created: ${new Date(caseData.created_at).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}`, MARGIN, chipY + 22);

      // ══════════════════════════════════════════════════════════
      // PAGE 2: AI FORENSIC ANALYSIS
      // ══════════════════════════════════════════════════════════
      doc.addPage();
      drawPageHeader('AI FORENSIC ANALYSIS');

      let y = 45;

      y = sectionLabel('AI FORENSIC EVALUATION SUMMARY', y);

      // Summary box with left accent bar
      const summaryText = analysis.ai_summary || 'No AI analysis available.';
      const summaryBoxH = Math.max(60, Math.ceil(summaryText.length / 80) * 14 + 20);
      doc.rect(MARGIN, y, 3, summaryBoxH).fill(c.accent);
      doc.rect(MARGIN + 3, y, CONTENT_W - 3, summaryBoxH).fill('#F0F4FF');
      doc.rect(MARGIN, y, CONTENT_W, summaryBoxH).stroke(c.border).lineWidth(0.5);
      doc.fillColor(c.textDark).font('Helvetica').fontSize(10)
         .text(summaryText, MARGIN + 15, y + 10, { width: CONTENT_W - 25, align: 'justify', lineGap: 3 });
      y += summaryBoxH + 20;

      // Brand impersonation alert
      if (brandImpersonated) {
        doc.rect(MARGIN, y, CONTENT_W, 28).fill(c.danger + '18');
        doc.rect(MARGIN, y, CONTENT_W, 28).stroke(c.danger).lineWidth(1);
        doc.fillColor(c.danger).font('Helvetica-Bold').fontSize(9)
           .text(`BRAND IMPERSONATION DETECTED: ${brandImpersonated.toUpperCase()}`, MARGIN + 10, y + 8);
        doc.font('Helvetica').fontSize(8).fillColor(c.textDark)
           .text('This site is impersonating a known brand. Users are likely being deceived.', MARGIN + 10, y + 18);
        y += 38;
      }

      // Threat indicators
      y = sectionLabel('THREAT INDICATORS', y);
      if (indicators.length === 0) {
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(c.muted).text('No specific indicators flagged.', MARGIN, y);
        y += 20;
      } else {
        indicators.forEach((ind, i) => {
          const bx = MARGIN + (i % 2) * (CONTENT_W / 2 + 5);
          const by = y + Math.floor(i / 2) * 22;
          doc.rect(bx, by, CONTENT_W / 2 - 5, 18).fill(c.danger + '12');
          doc.rect(bx, by, CONTENT_W / 2 - 5, 18).stroke(c.danger + '55').lineWidth(0.5);
          doc.fillColor(c.danger).font('Helvetica-Bold').fontSize(7)
             .text(`! ${ind}`, bx + 6, by + 4, { width: CONTENT_W / 2 - 18 });
        });
        y += Math.ceil(indicators.length / 2) * 22 + 12;
      }

      // Attack techniques
      y = sectionLabel('DETECTED ATTACK TECHNIQUES', y);
      if (techniques.length === 0) {
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(c.muted).text('No specific techniques identified.', MARGIN, y);
        y += 20;
      } else {
        techniques.forEach((tech, i) => {
          const tx = MARGIN + (i % 3) * (CONTENT_W / 3 + 2);
          const ty = y + Math.floor(i / 3) * 22;
          doc.rect(tx, ty, CONTENT_W / 3 - 4, 17).fill(c.warning + '15');
          doc.fillColor(c.warning).font('Helvetica').fontSize(8)
             .text(`• ${tech}`, tx + 6, ty + 4, { width: CONTENT_W / 3 - 16 });
        });
        y += Math.ceil(techniques.length / 3) * 22 + 12;
      }

      // MITRE ATT&CK table
      if (mitreTechniques.length > 0) {
        y = sectionLabel('MITRE ATT&CK TECHNIQUE MAPPING', y);
        const cols = [70, 200, 130, 105];
        y = drawTableHeader(['ID', 'Technique', 'Tactic', 'Relevance'], cols, MARGIN, y);
        mitreTechniques.forEach((m, i) => {
          const tactic = m.tactic || 'Unknown';
          y = drawTableRow(
            [m.id || 'N/A', m.name || 'Unknown', tactic, 'Direct Match'],
            cols, MARGIN, y, i,
            [c.accent, c.textDark, c.purple, c.subtle]
          );
        });
        y += 12;
      }

      // ══════════════════════════════════════════════════════════
      // PAGE 3: FORENSIC ARTIFACTS
      // ══════════════════════════════════════════════════════════
      doc.addPage();
      drawPageHeader('FORENSIC ARTIFACT ANALYSIS');
      y = 45;

      // WHOIS / IP Registry
      y = sectionLabel(caseData.target_type === 'IP' ? 'IP REGISTRY INFORMATION' : 'WHOIS DOMAIN REGISTRY', y);
      const whois = whoisData;
      const whoisRows = [
        [caseData.target_type === 'IP' ? 'Owner / ISP' : 'Registrar', whois.registrar],
        ['Country', whois.country],
        ['Domain Age', whois.ageDays != null ? `${whois.ageDays} days` : null],
        ['Created', whois.creationDate ? new Date(whois.creationDate).toLocaleDateString() : null],
        ['Expires', whois.expiryDate ? new Date(whois.expiryDate).toLocaleDateString() : null],
        ['Suspicious Age', whois.isSuspiciousAge ? 'YES — HIGH RISK' : 'No'],
      ];
      whoisRows.forEach(([label, value], i) => {
        const wx = MARGIN + (i % 2) * (CONTENT_W / 2 + 5);
        const wy = y + Math.floor(i / 2) * 30;
        doc.font('Helvetica-Bold').fontSize(8).fillColor(c.subtle).text(label + ':', wx, wy);
        const isRisk = label === 'Suspicious Age' && whois.isSuspiciousAge;
        doc.font('Helvetica').fontSize(9).fillColor(isRisk ? c.danger : c.textDark)
           .text(String(value || 'Unknown'), wx, wy + 11, { width: CONTENT_W / 2 - 10 });
      });
      y += Math.ceil(whoisRows.length / 2) * 30 + 15;

      // Network & SSL
      y = sectionLabel('NETWORK & SSL DETAILS', y);
      const geo = ipGeoData;
      const ssl = sslData;
      const netRows = [
        ['Resolved IP', geo.ip],
        ['Location', `${geo.city || '?'}, ${geo.country || '?'}`],
        ['ISP / Hosting', geo.isp],
        ['SSL Valid', caseData.target_type === 'IP' ? 'Skipped (IP)' : (ssl.valid === true ? 'Valid (Secure)' : 'INVALID / Missing')],
        ['SSL Issuer', caseData.target_type === 'IP' ? 'N/A' : ssl.issuer],
        ['SSL Expiry', caseData.target_type === 'IP' ? 'N/A' : (ssl.validTo ? ssl.validTo.toString().slice(0, 10) : null)],
      ];
      netRows.forEach(([label, value], i) => {
        const nx = MARGIN + (i % 2) * (CONTENT_W / 2 + 5);
        const ny = y + Math.floor(i / 2) * 30;
        doc.font('Helvetica-Bold').fontSize(8).fillColor(c.subtle).text(label + ':', nx, ny);
        const isRisk = label === 'SSL Valid' && ssl.valid !== true;
        doc.font('Helvetica').fontSize(9).fillColor(isRisk ? c.danger : c.textDark)
           .text(String(value || 'Unknown'), nx, ny + 11, { width: CONTENT_W / 2 - 10 });
      });
      y += Math.ceil(netRows.length / 2) * 30 + 15;

      // VirusTotal
      y = sectionLabel('VIRUSTOTAL MULTI-ENGINE SCAN', y);
      const vt = virustotalData;
      if (vt.error) {
        doc.font('Helvetica').fontSize(9).fillColor(c.danger).text(`VirusTotal Scan Failed: ${vt.error}`, MARGIN, y);
        y += 20;
      } else {
        const malCount = vt.maliciousCount || 0;
        const harmCount = vt.harmlessCount || 0;
        const suspCount = vt.suspiciousCount || 0;
        const totalEngines = vt.totalEngines || (malCount + harmCount + suspCount + (vt.undetectedCount || 0));

        // Summary bar
        doc.rect(MARGIN, y, CONTENT_W, 14).fill(c.lightBg);
        if (totalEngines > 0) {
          const malW = (malCount / totalEngines) * CONTENT_W;
          const suspW = (suspCount / totalEngines) * CONTENT_W;
          doc.rect(MARGIN, y, malW, 14).fill(c.danger);
          doc.rect(MARGIN + malW, y, suspW, 14).fill(c.warning);
        }
        doc.fillColor(c.white).font('Helvetica-Bold').fontSize(7.5)
           .text(`${malCount} Malicious  |  ${suspCount} Suspicious  |  ${harmCount} Harmless  |  Total: ${totalEngines} engines`, MARGIN + 8, y + 3);
        y += 22;

        const detections = vt.detections || [];
        if (detections.length > 0) {
          const dcols = [170, 150, 185];
          y = drawTableHeader(['Engine', 'Category', 'Result'], dcols, MARGIN, y);
          detections.slice(0, 10).forEach((d, i) => {
            const isMal = (d.result || '').toLowerCase().includes('phish') || (d.result || '').toLowerCase().includes('malware');
            y = drawTableRow(
              [d.engine || 'Unknown', d.category || 'malicious', d.result || 'flagged'],
              dcols, MARGIN, y, i,
              [c.textDark, c.textDark, isMal ? c.danger : c.warning]
            );
          });
          if (detections.length > 10) {
            doc.font('Helvetica-Oblique').fontSize(7.5).fillColor(c.muted)
               .text(`+ ${detections.length - 10} more detections not shown`, MARGIN, y + 4);
            y += 14;
          }
          y += 10;
        } else {
          doc.font('Helvetica').fontSize(9).fillColor(malCount > 0 ? c.danger : c.success)
             .text(malCount > 0 ? `${malCount} detections flagged. No breakdown available.` : 'No detections found by any engine.', MARGIN, y);
          y += 20;
        }
      }

      // Redirect chain
      if (y > 680) { doc.addPage(); drawPageHeader('FORENSIC ARTIFACT ANALYSIS (CONT.)'); y = 45; }
      y = sectionLabel('REDIRECT CHAIN TRACE', y);
      const chain = redirectChain;
      chain.forEach((url, i) => {
        const isFinal = i === chain.length - 1;
        const prefix = isFinal ? '[FINAL]' : `[HOP ${i + 1}]`;
        doc.font('Courier-Bold').fontSize(7.5).fillColor(isFinal ? c.danger : c.accent)
           .text(prefix, MARGIN, y);
        doc.font('Courier').fontSize(7.5).fillColor(c.textDark)
           .text(String(url), MARGIN + 55, y, { width: CONTENT_W - 55 });
        y += 14;
      });
      y += 10;

      // Screenshot
      const screenshot = screenshotData;
      if (screenshot && screenshot.startsWith('data:image/png;base64,')) {
        if (y > 600) { doc.addPage(); drawPageHeader('EVIDENCE SCREENSHOT'); y = 45; }
        y = sectionLabel('EVIDENCE SCREENSHOT (BROWSER SANDBOX CAPTURE)', y);
        try {
          const base64Data = screenshot.replace(/^data:image\/\w+;base64,/, '');
          const imgBuf = Buffer.from(base64Data, 'base64');
          const maxH = Math.min(300, 780 - y - 20);
          if (maxH > 40) {
            doc.image(imgBuf, MARGIN, y, { fit: [CONTENT_W, maxH], align: 'center', valign: 'center' });
            y += maxH + 15;
          }
        } catch (_) {
          doc.font('Helvetica-Oblique').fontSize(9).fillColor(c.muted)
             .text('Screenshot could not be rendered.', MARGIN, y);
          y += 15;
        }
      } else {
        if (y > 700) { doc.addPage(); drawPageHeader('FORENSIC ARTIFACT ANALYSIS (CONT.)'); y = 45; }
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(c.muted)
           .text('No screenshot was captured during sandbox analysis.', MARGIN, y);
        y += 15;
      }

      // ══════════════════════════════════════════════════════════
      // PAGE 4: CHAIN OF CUSTODY & LEGAL
      // ══════════════════════════════════════════════════════════
      doc.addPage();
      drawPageHeader('CHAIN OF CUSTODY & LEGAL CERTIFICATION');
      y = 45;

      y = sectionLabel('FORENSIC CHAIN OF CUSTODY', y);

      const custodyChain = data.custodyChain || [];
      if (custodyChain.length > 0) {
        const ccols = [105, 130, 100, 170];
        y = drawTableHeader(['Timestamp', 'Action', 'Analyst', 'SHA-256 Hash (After)'], ccols, MARGIN, y);
        custodyChain.forEach((entry, i) => {
          const ts = new Date(entry.timestamp).toLocaleString('en-US', {
            month: 'short', day: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
          });
          const hashShort = entry.hash_after
            ? entry.hash_after.substring(0, 20) + '...'
            : 'N/A';
          const analystName = entry.user?.name || 'System';
          y = drawTableRow(
            [ts, entry.action, analystName, hashShort],
            ccols, MARGIN, y, i,
            [c.subtle, c.textDark, c.textDark, c.accent]
          );
        });
        y += 12;
        doc.font('Helvetica').fontSize(8).fillColor(c.subtle)
           .text('Cryptographic SHA-256 hashes above establish tamper-evidence for each recorded event.', MARGIN, y, { width: CONTENT_W });
        y += 20;
      } else {
        doc.rect(MARGIN, y, CONTENT_W, 40).fill(c.lightBg);
        doc.font('Helvetica-Oblique').fontSize(9).fillColor(c.subtle)
           .text('No chain of custody records exist yet. Records are created when reports are compiled and analyses are performed.', MARGIN + 12, y + 12, { width: CONTENT_W - 24 });
        y += 50;
      }

      // Digital signature block
      y = sectionLabel('DIGITAL FORENSIC SIGNATURE (HMAC-SHA256)', y);
      doc.rect(MARGIN, y, CONTENT_W, 36).fill(c.dark);
      doc.font('Courier-Bold').fontSize(7).fillColor(c.accent)
         .text(data.digitalSignature || 'SIGNATURE UNAVAILABLE', MARGIN + 10, y + 8, { width: CONTENT_W - 20, wordBreak: true, ellipsis: true });
      doc.font('Helvetica').fontSize(7).fillColor(c.muted)
         .text(`Version: ${data.version || 1}  |  ${new Date(data.generated_at).toUTCString()}`, MARGIN + 10, y + 24);
      y += 46;

      // Legal disclaimer
      y = sectionLabel('LEGAL NOTICE & CERTIFICATION', y);
      const legalText = `This document is a confidential forensic report generated by PhishTrack, an automated cyber intelligence platform. The digital signature embedded above (HMAC-SHA256) cryptographically binds the case metadata, threat assessment, and forensic evidence to this specific report version. Any modification to the content of this document will invalidate the cryptographic signature and will be detectable via the VERIFY function.

This report may be used as a legally admissible forensic artifact in cybersecurity investigations, law enforcement proceedings, and incident response documentation. The chain of custody records above provide a verifiable audit trail for all case lifecycle events.

Classification: CONFIDENTIAL — FOR AUTHORIZED PERSONNEL ONLY.
Analyst: ${analyst.name || 'System'}  |  Organization: ${analyst.organization || caseData.organization || 'PhishTrack SOC'}`;

      doc.rect(MARGIN, y, CONTENT_W, 110).fill(c.lightBg);
      doc.rect(MARGIN, y, CONTENT_W, 110).stroke(c.border).lineWidth(0.5);
      doc.font('Helvetica').fontSize(8.5).fillColor(c.textDark)
         .text(legalText, MARGIN + 12, y + 10, { width: CONTENT_W - 24, align: 'justify', lineGap: 2 });
      y += 120;

      // ── Footer on every page ──────────────────────────────────
      const range = doc.bufferedPageRange();
      for (let i = 0; i < range.count; i++) {
        doc.switchToPage(i);
        // Bottom separator
        doc.rect(0, 822, W, 20).fill(c.dark);
        doc.fillColor(c.muted).font('Helvetica').fontSize(6.5)
           .text(
             `PhishTrack Forensic Report  |  Case: ${caseData.case_number || 'N/A'}  |  Generated: ${new Date(data.generated_at).toUTCString()}  |  Page ${i + 1} of ${range.count}`,
             MARGIN, 828, { width: W - MARGIN * 2, align: 'center' }
           );
      }

      doc.end();
    } catch (err) {
      reject(err);
    }
  });
};