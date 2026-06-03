/**
 * Validates DATABASE_URL format and tests a live Postgres connection via Prisma.
 *
 * Usage:
 *   node scripts/check-database-url.js
 *   npm run check:db
 */
const dotenv = require('dotenv');
const path = require('path');

dotenv.config({ path: path.join(__dirname, '../.env') });

const { PrismaClient } = require('@prisma/client');

function maskDatabaseUrl(rawUrl) {
  try {
    const url = new URL(rawUrl);
    if (url.password) url.password = '****';
    return url.toString();
  } catch {
    return '(invalid URL — could not parse)';
  }
}

function analyzeDatabaseUrl(rawUrl) {
  const issues = [];
  const hints = [];

  if (!rawUrl || String(rawUrl).trim() === '') {
    return {
      ok: false,
      issues: ['DATABASE_URL is missing or empty.'],
      hints: ['Copy .env.example to .env and set DATABASE_URL.'],
      summary: null,
    };
  }

  let url;
  try {
    url = new URL(rawUrl);
  } catch (err) {
    return {
      ok: false,
      issues: [`DATABASE_URL is not a valid URL: ${err.message}`],
      hints: ['Use format: postgresql://user:password@host:port/database?options'],
      summary: null,
    };
  }

  const protocol = url.protocol.replace(':', '');
  if (protocol !== 'postgresql' && protocol !== 'postgres') {
    issues.push(`Unexpected protocol "${protocol}". Expected postgresql:// or postgres://.`);
  }

  const host = url.hostname;
  const port = url.port || '5432';
  const database = url.pathname.replace(/^\//, '') || '(default)';
  const hasPgbouncer = url.searchParams.get('pgbouncer') === 'true';
  const sslMode = url.searchParams.get('sslmode');
  const isSupabase = host.includes('supabase.co') || host.includes('supabase.com');

  const summary = {
    host,
    port,
    database,
    user: url.username || '(none)',
    sslmode: sslMode || '(not set)',
    pgbouncer: hasPgbouncer,
    masked: maskDatabaseUrl(rawUrl),
  };

  if (!url.username) {
    issues.push('DATABASE_URL has no username.');
  }
  if (!url.password) {
    issues.push('DATABASE_URL has no password.');
  }
  if (!host) {
    issues.push('DATABASE_URL has no host.');
  }

  if (isSupabase) {
    const isDirect = host.startsWith('db.') && port === '5432';
    const isPooler = host.includes('pooler') || port === '6543';

    if (isDirect) {
      issues.push('Using Supabase direct connection (db.*.supabase.co:5432).');
      hints.push(
        'Render and other serverless hosts often cannot reach the direct host.',
        'In Supabase → Project Settings → Database, copy the Transaction pooler URI (port 6543).',
        'Append ?pgbouncer=true&connection_limit=1 for Prisma.'
      );
    }

    if (isPooler && !hasPgbouncer) {
      issues.push('Supabase pooler URL detected but pgbouncer=true is missing.');
      hints.push('Add ?pgbouncer=true&connection_limit=1 to DATABASE_URL for Prisma.');
    }

    if (!sslMode) {
      hints.push('Consider adding ?sslmode=require if connections fail with SSL errors.');
    }
  }

  if (port === '5432' && process.env.RENDER) {
    hints.push('Running on Render: prefer a connection pooler (port 6543) over direct port 5432.');
  }

  return {
    ok: issues.length === 0,
    issues,
    hints,
    summary,
  };
}

async function testConnection() {
  const prisma = new PrismaClient();
  const started = Date.now();

  try {
    await prisma.$connect();
    const rows = await prisma.$queryRaw`SELECT 1 AS ok, current_database() AS database, version() AS version`;
    const elapsedMs = Date.now() - started;
    const row = Array.isArray(rows) ? rows[0] : rows;

    return {
      ok: true,
      elapsedMs,
      database: row?.database ?? '(unknown)',
      version: row?.version ?? '(unknown)',
    };
  } catch (err) {
    return {
      ok: false,
      elapsedMs: Date.now() - started,
      error: err.message || String(err),
    };
  } finally {
    await prisma.$disconnect().catch(() => {});
  }
}

function printSection(title) {
  console.log(`\n== ${title} ==`);
}

(async () => {
  const rawUrl = process.env.DATABASE_URL;

  printSection('DATABASE_URL analysis');
  const analysis = analyzeDatabaseUrl(rawUrl);

  if (analysis.summary) {
    console.log('Masked URL :', analysis.summary.masked);
    console.log('Host       :', analysis.summary.host);
    console.log('Port       :', analysis.summary.port);
    console.log('Database   :', analysis.summary.database);
    console.log('User       :', analysis.summary.user);
    console.log('sslmode    :', analysis.summary.sslmode);
    console.log('pgbouncer  :', analysis.summary.pgbouncer);
  }

  if (analysis.issues.length) {
    console.log('\nWarnings:');
    analysis.issues.forEach((msg) => console.log('  •', msg));
  }

  if (analysis.hints.length) {
    console.log('\nHints:');
    analysis.hints.forEach((msg) => console.log('  →', msg));
  }

  if (!rawUrl || !rawUrl.trim()) {
    process.exitCode = 1;
    return;
  }

  printSection('Connection test');
  console.log('Connecting via Prisma...');
  const result = await testConnection();

  if (result.ok) {
    console.log('Status     : OK');
    console.log('Latency    :', `${result.elapsedMs} ms`);
    console.log('Database   :', result.database);
    console.log('Postgres   :', result.version.split('\n')[0]);
    console.log('\nDATABASE_URL check passed.');
    process.exitCode = analysis.issues.length ? 0 : 0;
    return;
  }

  console.log('Status     : FAILED');
  console.log('Latency    :', `${result.elapsedMs} ms`);
  console.log('Error      :', result.error);
  console.log('\nDATABASE_URL check failed — fix the URL or ensure the database is running.');
  process.exitCode = 1;
})();
