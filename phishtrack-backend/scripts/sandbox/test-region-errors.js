const { PrismaClient } = require('@prisma/client');
const regions = ['ap-south-1', 'us-east-1', 'eu-central-1'];
async function main() {
  for (const region of regions) {
    const url = "postgresql://postgres.dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-0-" + region + ".pooler.supabase.com:6543/postgres?pgbouncer=true";
    const prisma = new PrismaClient({ datasources: { db: { url } } });
    try {
      await prisma.$connect();
      console.log("SUCCESS on", region);
      return;
    } catch (e) {
      console.log("FAILED on", region, ":", e.message.split('\n')[0]);
    }
  }
}
main();
