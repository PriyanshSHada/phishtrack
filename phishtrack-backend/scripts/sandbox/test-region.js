const { PrismaClient } = require('@prisma/client');

const regions = [
  'ap-south-1',
  'us-east-1',
  'us-east-2',
  'us-west-1',
  'us-west-2',
  'eu-central-1',
  'eu-west-1',
  'eu-west-2',
  'eu-west-3',
  'ap-southeast-1',
  'ap-southeast-2',
  'ap-northeast-1',
  'ap-northeast-2',
  'sa-east-1',
  'ca-central-1'
];

async function checkRegion(region) {
  const url = "postgresql://postgres.dtpalxyiasrwnxoztcyr:Kannu323001%23@aws-0-" + region + ".pooler.supabase.com:6543/postgres?pgbouncer=true";
  const prisma = new PrismaClient({ datasources: { db: { url } } });
  try {
    await prisma.$connect();
    console.log("SUCCESS: Found database in region " + region);
    await prisma.$disconnect();
    return url;
  } catch (e) {
    if (e.message.includes('password authentication failed') || e.message.includes('password')) {
      console.log("WRONG PASSWORD but correct region: " + region);
      return url;
    }
  }
  return null;
}

async function main() {
  for (const region of regions) {
    const found = await checkRegion(region);
    if (found) {
      console.log("WORKING URL:", found);
      process.exit(0);
    }
  }
  console.log("Could not find the correct region.");
}

main();
