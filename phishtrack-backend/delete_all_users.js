const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  console.log('Deleting all Analysis records...');
  await prisma.analysis.deleteMany();
  
  console.log('Deleting all Report records...');
  await prisma.report.deleteMany();
  
  console.log('Deleting all ChainOfCustody records...');
  await prisma.chainOfCustody.deleteMany();
  
  console.log('Deleting all AuditLog records...');
  await prisma.auditLog.deleteMany();
  
  console.log('Deleting all Case records...');
  await prisma.case.deleteMany();
  
  console.log('Deleting all User records...');
  const deletedUsers = await prisma.user.deleteMany();
  
  console.log(`Successfully deleted ${deletedUsers.count} user profiles and all associated data in the database.`);
}

main()
  .catch(e => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
