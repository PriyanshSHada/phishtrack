/**
 * Runs `prisma migrate deploy` using a migration-friendly connection URL.
 * Transaction pooler (6543 + pgbouncer) can hang on migrations; this tries
 * direct and session pooler URLs derived from DATABASE_URL in .env.
 */
const dotenv = require('dotenv');
const path = require('path');
const { spawnSync } = require('child_process');

dotenv.config({ path: path.join(__dirname, '../.env') });

const rawUrl = process.env.DATABASE_URL;
if (!rawUrl) {
  console.error('DATABASE_URL is not set in .env');
  process.exit(1);
}

const url = new URL(rawUrl);
const projectRef = url.username.startsWith('postgres.')
  ? url.username.replace('postgres.', '')
  : null;

const candidates = [];

if (projectRef) {
  candidates.push({
    name: 'direct (db.*.supabase.co:5432)',
    url: `postgresql://postgres:${url.password}@db.${projectRef}.supabase.co:5432/postgres?sslmode=require`,
  });
  candidates.push({
    name: 'session pooler (:5432)',
    url: `postgresql://${url.username}:${url.password}@${url.hostname}:5432/postgres?sslmode=require`,
  });
}

candidates.push({
  name: 'DATABASE_URL from .env',
  url: rawUrl,
});

for (const candidate of candidates) {
  console.log(`\n== migrate deploy via ${candidate.name} ==`);
  const result = spawnSync('npx', ['prisma', 'migrate', 'deploy'], {
    env: { ...process.env, DATABASE_URL: candidate.url },
    stdio: 'inherit',
    shell: true,
    timeout: 120000,
  });

  if (result.status === 0) {
    console.log('\nMigrations applied successfully.');
    process.exit(0);
  }

  if (result.error) {
    console.error('Process error:', result.error.message);
  }
}

console.error('\nAll migration attempts failed.');
process.exit(1);
