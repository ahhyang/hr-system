# PulseHR — AI Employee Management System

AI-powered HR operating system for Malaysian SMEs: attendance, leave, payroll, claims, reports, and an HR assistant.

Register a company, add employees, clock in, apply leave, run payroll, then ask AI:

> “How many annual leave days do I have left?”

## Local run

**Database** (Docker Desktop):

```bash
docker compose up -d
```

Or set `DATABASE_URL` to a Neon pooled connection string.

**Backend**

```bash
cd backend
# Optional: OpenRouter key in application-local.yml (gitignored) or OPENROUTER_API_KEY
./mvnw spring-boot:run
```

**Frontend**

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173 → **Create your workspace**.

## Publish (free)

1. **Neon** — create Postgres, run `CREATE EXTENSION IF NOT EXISTS vector;`, copy the **pooler** URL.
2. **Render** — Web Service, root `backend`, build `./mvnw -DskipTests package`, start `java -jar target/hr-system-0.1.0-SNAPSHOT.jar`.
   Env: `DATABASE_URL`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `OPENROUTER_API_KEY`.
3. **Vercel** — root `frontend`, env `VITE_API_URL=https://YOUR-API.onrender.com/api/v1`.

Do not commit API keys. Rotate any key that was pasted in chat.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | React 19 + Vite + TypeScript + Tailwind → **Vercel** |
| Backend | Spring Boot 3.2 + Java 21 → **Render** |
| Database | PostgreSQL + pgvector → **Neon** |
| Files | Cloudflare R2 (optional) |
| Cron | GitHub Actions |

## Quick Start (Local)

### 1. Database

```bash
docker compose up -d
```

Or use a [Neon](https://neon.tech) connection string.

### 2. Backend

```bash
cd backend
# Set DATABASE_URL or use defaults (localhost:5432/hrsystem)
./mvnw spring-boot:run
```

API: http://localhost:8080  
Health: http://localhost:8080/actuator/health

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

App: http://localhost:5173

## Deploy (Free Tier)

### Neon
1. Create project at [neon.tech](https://neon.tech)
2. Enable `pgvector` extension: `CREATE EXTENSION vector;`
3. Copy **pooled** connection string → `DATABASE_URL`

### Render (Backend)
1. Connect GitHub repo
2. Use `render.yaml` or set:
   - Root: `backend`
   - Build: `./mvnw -DskipTests package`
   - Start: `java -jar target/hr-system-0.1.0-SNAPSHOT.jar`
3. Set env: `DATABASE_URL`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `CRON_SECRET`

### Vercel (Frontend)
1. Import repo, set root directory: `frontend`
2. Env: `VITE_API_URL=https://your-api.onrender.com/api/v1`

### GitHub Actions Cron
Set secrets: `RENDER_API_URL`, `CRON_SECRET`

## Roles

- **Employee** — Clock in/out, leave, claims, payslips, AI assistant
- **Manager** — Team view, approve leave/claims
- **HR** — Full employee management, payroll, reports
- **Company Admin** — Tenant settings + all HR permissions

## API Overview

```
POST /api/v1/auth/register-tenant
POST /api/v1/auth/login
GET  /api/v1/me/dashboard
POST /api/v1/attendance/clock-in
POST /api/v1/attendance/clock-out
POST /api/v1/leaves
POST /api/v1/ai/chat
GET  /api/v1/hr/dashboard
POST /api/v1/cron/daily  (X-Cron-Secret header)
```

## License

Private — All rights reserved.
