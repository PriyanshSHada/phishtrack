const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function clean() {
  try {
    console.log('Deleting all cases (which cascades to analyses, reports, etc if configured)...');
    await prisma.analysis.deleteMany({});
    await prisma.report.deleteMany({});
    await prisma.chainOfCustody.deleteMany({});
    await prisma.auditLog.deleteMany({});
    await prisma.case.deleteMany({});
    
    console.log('Deleting all users...');
    await prisma.user.deleteMany({});
    
    console.log('Database successfully cleared of all old registration and case data.');
  } catch (error) {
    console.error('Failed to clean database:', error);
  } finally {
    await prisma.$disconnect();
  }
}

clean();
