const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const email = 'priyanshsinghhada@gmail.com';
  const user = await prisma.user.findUnique({ where: { email } });
  
  if (!user) {
    console.log('User not found.');
    return;
  }
  
  const userId = user.id;
  
  console.log('Deleting Analysis records...');
  await prisma.analysis.deleteMany({ where: { case: { userId } } });
  
  console.log('Deleting Report records...');
  await prisma.report.deleteMany({ where: { case: { userId } } });
  
  console.log('Deleting ChainOfCustody records...');
  await prisma.chainOfCustody.deleteMany({ where: { case: { userId } } });
  
  console.log('Deleting AuditLog records...');
  // Also delete audit logs related directly to the user (e.g. login/logout logs)
  await prisma.auditLog.deleteMany({ where: { userId } });
  await prisma.auditLog.deleteMany({ where: { case: { userId } } });
  
  console.log('Deleting Case records...');
  const deletedCases = await prisma.case.deleteMany({ where: { userId } });
  
  console.log(`Deleted ${deletedCases.count} cases and all associated data for ${email}.`);
}

main()
  .catch(e => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
