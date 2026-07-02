const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: 'postgresql://postgres.dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-0-ap-south-1.pooler.supabase.com:6543/postgres?pgbouncer=true'
    }
  }
});

async function main() {
  try {
    await prisma.$connect();
    console.log("SUCCESS: Connected to database");
    await prisma.$disconnect();
  } catch (e) {
    console.error("ERROR:", e);
  }
}
main();
