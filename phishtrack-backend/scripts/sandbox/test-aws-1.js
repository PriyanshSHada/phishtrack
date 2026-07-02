const { PrismaClient } = require('@prisma/client');
const url = "postgresql://postgres.dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-1-ap-south-1.pooler.supabase.com:6543/postgres?pgbouncer=true";
const prisma = new PrismaClient({ datasources: { db: { url } } });
async function main() {
  try {
    await prisma.$connect();
    console.log("SUCCESS on aws-1!");
    return;
  } catch (e) {
    console.log("FAILED on aws-1:", e.message.split('\n')[0]);
  }
}
main();
