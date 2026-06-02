const PDFDocument = require('pdfkit');
const fs = require('fs');

exports.generatePdfReport = (outputPath, data) => {
  return new Promise((resolve, reject) => {
    try {
      const doc = new PDFDocument({ margin: 50, size: 'A4' });

      const stream = fs.createWriteStream(outputPath);
      doc.pipe(stream);

      // Colors
      const primaryColor = '#141829';
      const accentColor = '#00B4D8';
      const dangerColor = '#FF3B3B';
      const warningColor = '#FFA500';
      const successColor = '#00FF88';
      const textColor = '#333333';
      const subtextColor = '#666666';
      
      // Page background accent
      doc.rect(0, 0, 595.28, 20).fill(primaryColor);

      // Header Title
      doc.fillColor(primaryColor)
         .font('Helvetica-Bold')
         .fontSize(24)
         .text('PhishTrack Forensics', 50, 40);

      doc.fillColor(subtextColor)
         .font('Helvetica')
         .fontSize(10)
         .text('Phishing Link Investigation & Forensic Reporting', 50, 68);

      doc.moveDown(1.5);

      // Horizontal separator line
      doc.moveTo(50, 85).lineTo(545, 85).strokeColor('#E0E0E0').lineWidth(1).stroke();

      // Case Metadata Box
      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(14).text('CASE SUMMARY', 50, 100);
      
      doc.font('Helvetica').fontSize(10).fillColor(textColor);
      
      const caseData = data.case;
      const analysis = data.analysis;
      const analyst = data.analyst;

      doc.text(`Case Number: ${caseData.case_number}`, 50, 125);
      doc.text(`Created Date: ${new Date(caseData.created_at).toUTCString()}`, 50, 140);
      doc.text(`Source Channel: ${caseData.source}`, 50, 155);
      doc.text(`Case Status: ${caseData.status}`, 50, 170);

      doc.text(`Target URL: ${caseData.url}`, 300, 125, { width: 245 });
      doc.text(`Priority Level: ${caseData.priority}`, 300, 155);
      doc.text(`Lead Analyst: ${analyst ? analyst.name : 'System Assigned'} (${analyst ? analyst.email : ''})`, 300, 170);

      doc.moveDown(2);

      // Threat Score Section
      const score = analysis ? (analysis.threat_score || 0) : 0;
      let scoreColor = successColor;
      if (score >= 40 && score < 70) scoreColor = warningColor;
      if (score >= 70) scoreColor = dangerColor;

      doc.rect(50, 195, 495, 60).fill('#F8F9FA');
      
      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(12).text('THREAT ASSESSMENT', 65, 205);
      
      doc.fillColor(scoreColor).font('Helvetica-Bold').fontSize(24).text(`${score}/100`, 65, 222);
      
      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(12).text(`Severity: ${analysis ? (analysis.severity || 'Low') : 'Low'}`, 180, 225);

      doc.moveDown(3.5);

      // AI Summary Description
      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(14).text('ASSESSMENT SUMMARY', 50, 275);
      doc.font('Helvetica').fontSize(10).fillColor(textColor)
         .text(analysis ? (analysis.ai_summary || 'No summary available.') : 'No summary available.', 50, 295, { width: 495, align: 'justify', lineGap: 3 });

      doc.moveDown(2);

      // Indicators found
      let currentY = doc.y + 10;
      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(14).text('THREAT INDICATORS DETECTED', 50, currentY);
      currentY += 20;

      const indicators = analysis ? (analysis.ai_indicators || []) : [];
      if (indicators.length === 0) {
        doc.font('Helvetica-Oblique').fontSize(10).fillColor(subtextColor).text('No malicious indicators flagged.', 50, currentY);
        currentY += 15;
      } else {
        doc.font('Helvetica').fontSize(10).fillColor(textColor);
        indicators.forEach(ind => {
          doc.text(`• ${ind}`, 60, currentY);
          currentY += 15;
        });
      }

      // Add a page break for detailed forensics and screenshot
      doc.addPage();
      doc.rect(0, 0, 595.28, 20).fill(primaryColor);

      doc.fillColor(primaryColor).font('Helvetica-Bold').fontSize(14).text('DETAILED FORENSIC ARTIFACTS', 50, 40);

      // WHOIS, SSL, VT, Similarity details
      let detailsY = 65;
      
      doc.fontSize(10).font('Helvetica-Bold').fillColor(primaryColor).text('WHOIS Registrar Data:', 50, detailsY);
      doc.font('Helvetica').fillColor(textColor)
         .text(`Registrar: ${analysis?.whois_data?.registrar || 'Unknown'} | Country: ${analysis?.whois_data?.country || 'Unknown'}`, 50, detailsY + 15);
      doc.text(`Domain Age: ${analysis?.whois_data?.ageDays !== null && analysis?.whois_data?.ageDays !== undefined ? analysis.whois_data.ageDays + ' days' : 'Unknown'} (Creation Date: ${analysis?.whois_data?.creationDate ? new Date(analysis.whois_data.creationDate).toLocaleDateString() : 'N/A'})`, 50, detailsY + 28);
      
      detailsY += 50;

      doc.font('Helvetica-Bold').fillColor(primaryColor).text('Network & SSL details:', 50, detailsY);
      doc.font('Helvetica').fillColor(textColor)
         .text(`Resolved IP Address: ${analysis?.ip_geolocation?.ip || 'N/A'} (Location: ${analysis?.ip_geolocation?.city || 'Unknown'}, ${analysis?.ip_geolocation?.country || 'Unknown'} - ISP: ${analysis?.ip_geolocation?.isp || 'Unknown'})`, 50, detailsY + 15);
      doc.text(`SSL Certificate Valid: ${analysis?.ssl_info?.valid === true ? 'Yes' : 'No'} (Issuer: ${analysis?.ssl_info?.issuer || 'N/A'})`, 50, detailsY + 28);

      detailsY += 50;

      doc.font('Helvetica-Bold').fillColor(primaryColor).text('External Scan Results:', 50, detailsY);
      doc.font('Helvetica').fillColor(textColor)
         .text(`VirusTotal Engines: ${analysis?.virustotal_result?.maliciousCount || 0} malicious detections out of 70+ security vendors.`, 50, detailsY + 15);

      detailsY += 40;

      // Screenshot Title
      doc.font('Helvetica-Bold').fillColor(primaryColor).text('EVIDENCE SCREENSHOT CAPTURE', 50, detailsY);
      detailsY += 20;

      // Add Screenshot image if available
      const screenshot = analysis?.page_screenshot;
      if (screenshot && screenshot.startsWith('data:image/png;base64,')) {
        try {
          const base64Data = screenshot.replace('data:image/png;base64,', '');
          const buffer = Buffer.from(base64Data, 'base64');
          doc.image(buffer, 50, detailsY, { width: 495, height: 260 });
          detailsY += 270;
        } catch (imgErr) {
          doc.font('Helvetica-Oblique').fillColor(dangerColor).text('Failed to render screenshot image buffer.', 50, detailsY);
          detailsY += 20;
        }
      } else {
        doc.font('Helvetica-Oblique').fillColor(subtextColor).text('No screenshot captured or available.', 50, detailsY);
        detailsY += 25;
      }

      // Digital Signature Footer
      doc.moveTo(50, 780).lineTo(545, 780).strokeColor('#E0E0E0').lineWidth(1).stroke();
      
      doc.fillColor(subtextColor)
         .font('Helvetica-Bold')
         .fontSize(8)
         .text('DIGITAL FORENSIC SIGNATURE (HMAC-SHA256):', 50, 790);

      doc.font('Helvetica')
         .text(data.digitalSignature || 'None', 50, 802, { width: 495, wordBreak: true });

      doc.font('Helvetica')
         .text(`Report Version: ${data.version || 1} | Generated At: ${new Date(data.generated_at).toUTCString()}`, 50, 818);

      doc.end();

      stream.on('finish', () => {
        resolve();
      });

      stream.on('error', (err) => {
        reject(err);
      });
    } catch (err) {
      reject(err);
    }
  });
};
