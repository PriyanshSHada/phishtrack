const { PrismaClient } = require('@prisma/client');
const url = "postgresql://postgres:Kannu323001%23@db.dtpalxyiasrwnxoztcyr.supabase.co:5432/postgres";
const prisma = new PrismaClient({ datasources: { db: { url } } });
async function main() {
  try {
    await prisma.$connect();
    console.log("SUCCESS: Connected to direct DB!");
    await prisma.$disconnect();
  } catch (e) {
    console.error("ERROR:", e);
  }
}
main();
