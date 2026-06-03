# PhishTrack Backend

Minimal scaffold for the PhishTrack backend service.

Quick start:

1. Install dependencies

```bash
cd phishtrack-backend
npm install
```

2. Copy `.env.example` to `.env` and update values

3. Run in development

```bash
npm run dev
```

The server exposes `GET /api/health`.

4. Verify database connectivity

```bash
npm run check:db
```

This validates `DATABASE_URL` format (including Supabase pooler hints) and runs a live Prisma connection test.
