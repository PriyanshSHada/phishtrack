const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const prisma = new PrismaClient();

async function main() {
  try {
    const a = await prisma.$queryRaw`SELECT confidence, verdict, mitre_techniques, brand_impersonated FROM "Analysis" LIMIT 1`;
    fs.writeFileSync('db-check-result.txt', 'SUCCESS: Columns exist!');
  } catch (e) {
    fs.writeFileSync('db-check-result.txt', 'ERROR: ' + e.message);
  }
}
main().catch(console.error).finally(()=>prisma.$disconnect());
