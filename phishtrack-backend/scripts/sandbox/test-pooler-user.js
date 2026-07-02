const { PrismaClient } = require('@prisma/client');
const url = "postgresql://dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-0-ap-south-1.pooler.supabase.com:6543/postgres?pgbouncer=true";
const prisma = new PrismaClient({ datasources: { db: { url } } });
async function main() {
  try {
    await prisma.$connect();
    console.log("SUCCESS: Connected!");
    await prisma.$disconnect();
  } catch (e) {
    console.error("ERROR:", e.message);
  }
}
main();
