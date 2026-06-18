const { createClient } = require('@supabase/supabase-js');
const logger = require('../utils/logger');

const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_KEY || '';

let supabase = null;
if (supabaseUrl && supabaseKey) {
  supabase = createClient(supabaseUrl, supabaseKey);
  logger.info('Supabase storage client initialized');
  
  // Ensure the 'reports' bucket exists and is private
  supabase.storage.getBucket('reports').then(({ data, error }) => {
    if (error && error.message.includes('not found')) {
      logger.info("Creating 'reports' bucket in Supabase Storage...");
      supabase.storage.createBucket('reports', { public: false })
        .catch(err => logger.error('Failed to create bucket:', err));
    }
  });
} else {
  logger.warn('Supabase credentials missing.');
}

exports.uploadReportPdf = async (reportId, fileBuffer) => {
  if (!supabase) return null;
  
  try {
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
    
    // Return the storage path for the DB
    return `reports/${fileName}`;
  } catch (err) {
    logger.error('Error during Supabase upload:', err);
    return null;
  }
};

exports.getSignedUrl = async (fileName) => {
  if (!supabase) return null;
  
  try {
    // Generate a signed URL valid for 1 hour (3600 seconds)
    const { data, error } = await supabase.storage
      .from('reports')
      .createSignedUrl(fileName, 3600);
      
    if (error) {
      logger.error(`Failed to generate signed URL: ${error.message}`);
      return null;
    }
    
    return data.signedUrl;
  } catch (err) {
    logger.error('Error generating signed URL:', err);
    return null;
  }
};

exports.downloadReportPdf = async (fileName) => {
  if (!supabase) return null;
  
  try {
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
