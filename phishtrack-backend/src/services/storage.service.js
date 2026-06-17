const { createClient } = require('@supabase/supabase-js');
const logger = require('../utils/logger');
const fs = require('fs');

const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_KEY || '';

let supabase = null;
if (supabaseUrl && supabaseKey) {
  supabase = createClient(supabaseUrl, supabaseKey);
  logger.info('Supabase storage client initialized');
} else {
  logger.warn('Supabase credentials missing, falling back to local file storage for PDFs.');
}

exports.uploadReportPdf = async (reportId, filePath) => {
  if (!supabase) return null;
  
  try {
    const fileBuffer = fs.readFileSync(filePath);
    const fileName = `${reportId}.pdf`;
    
    const { data, error } = await supabase.storage
      .from('reports')
      .upload(fileName, fileBuffer, {
        contentType: 'application/pdf',
        upsert: true
      });
      
    if (error) {
      logger.error(`Failed to upload PDF to Supabase Storage: ${error.message}`);
      return null;
    }
    
    // Get public URL
    const { data: publicUrlData } = supabase.storage
      .from('reports')
      .getPublicUrl(fileName);
      
    return publicUrlData.publicUrl;
  } catch (err) {
    logger.error('Error during Supabase upload:', err);
    return null;
  }
};

exports.downloadReportPdf = async (reportId) => {
  if (!supabase) return null;
  
  try {
    const fileName = `${reportId}.pdf`;
    const { data, error } = await supabase.storage
      .from('reports')
      .download(fileName);
      
    if (error) {
      logger.error(`Failed to download PDF from Supabase Storage: ${error.message}`);
      return null;
    }
    
    const buffer = Buffer.from(await data.arrayBuffer());
    return buffer;
  } catch (err) {
    logger.error('Error during Supabase download:', err);
    return null;
  }
};
