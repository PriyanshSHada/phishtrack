const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: "postgresql://postgres.dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-1-ap-south-1.pooler.supabase.com:6543/postgres?pgbouncer=true"
    }
  }
});

async function main() {
  const c = await prisma.case.findFirst({
    include: { analyses: true, reports: true, auditLogs: true },
    orderBy: { created_at: 'desc' }
  });
  console.log(JSON.stringify(c, null, 2));
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
