const { PrismaClient } = require('@prisma/client');
const readline = require('readline');

const prisma = new PrismaClient();

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

async function main() {
  // Configurable date threshold
  const dateThreshold = new Date('2026-06-01T00:00:00Z');

  console.log(`Searching for test cases created before ${dateThreshold.toISOString()} OR with title containing 'test'...`);

  // 1. Find the cases that match our criteria
  const casesToDelete = await prisma.case.findMany({
    where: {
      OR: [
        { created_at: { lt: dateThreshold } },
        { title: { contains: 'test', mode: 'insensitive' } }
      ]
    },
    select: {
      id: true,
      title: true,
      created_at: true
    }
  });

  if (casesToDelete.length === 0) {
    console.log('No cases found matching the criteria. Exiting.');
    process.exit(0);
  }

  const caseIds = casesToDelete.map(c => c.id);

  console.log(`Found ${casesToDelete.length} cases to delete.`);
  // Optional: print a few of them for context
  casesToDelete.slice(0, 5).forEach(c => {
    console.log(` - ID: ${c.id} | Title: "${c.title}" | Created At: ${c.created_at.toISOString()}`);
  });
  if (casesToDelete.length > 5) {
    console.log(`   ...and ${casesToDelete.length - 5} more.`);
  }

  // 2. Dry-run: count related records that will be deleted
  const analysisCount = await prisma.analysis.count({ where: { caseId: { in: caseIds } } });
  const reportCount = await prisma.report.count({ where: { caseId: { in: caseIds } } });
  const auditLogCount = await prisma.auditLog.count({ where: { caseId: { in: caseIds } } });
  const custodyCount = await prisma.chainOfCustody.count({ where: { caseId: { in: caseIds } } });

  console.log('\n--- DRY RUN: The following records will be deleted ---');
  console.log(`Cases: ${casesToDelete.length}`);
  console.log(`Analyses: ${analysisCount}`);
  console.log(`Reports: ${reportCount}`);
  console.log(`AuditLogs: ${auditLogCount}`);
  console.log(`ChainOfCustody: ${custodyCount}`);
  console.log('------------------------------------------------------\n');

  rl.question('Are you sure you want to PERMANENTLY delete these records? (yes/no): ', async (answer) => {
    if (answer.toLowerCase() === 'yes' || answer.toLowerCase() === 'y') {
      try {
        console.log('\nDeleting dependent records first to maintain foreign key constraints...');
        
        // Delete in order to avoid FK constraint errors
        await prisma.analysis.deleteMany({ where: { caseId: { in: caseIds } } });
        console.log(`Deleted ${analysisCount} Analysis records.`);

        await prisma.report.deleteMany({ where: { caseId: { in: caseIds } } });
        console.log(`Deleted ${reportCount} Report records.`);

        await prisma.auditLog.deleteMany({ where: { caseId: { in: caseIds } } });
        console.log(`Deleted ${auditLogCount} AuditLog records.`);

        await prisma.chainOfCustody.deleteMany({ where: { caseId: { in: caseIds } } });
        console.log(`Deleted ${custodyCount} ChainOfCustody records.`);

        // Finally, delete the cases
        console.log('\nDeleting the cases...');
        const caseDeleteResult = await prisma.case.deleteMany({ where: { id: { in: caseIds } } });
        console.log(`Deleted ${caseDeleteResult.count} Case records.`);

        console.log('\nCleanup successful.');
      } catch (error) {
        console.error('An error occurred during deletion:', error);
      } finally {
        await prisma.$disconnect();
        rl.close();
      }
    } else {
      console.log('Deletion cancelled.');
      await prisma.$disconnect();
      rl.close();
    }
  });
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
