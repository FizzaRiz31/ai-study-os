# AI Study OS

A full-stack student productivity platform that helps students manage assignments, exams, and study time using a custom priority-scoring algorithm and AI-generated weekly study plans.

Built as a personal project to learn full-stack development with Spring Boot, React, and the OpenAI API.

## Why I built this

I kept missing assignments because my planner couldn't tell me what to do *first*. So I built one that ranks every task with a priority score (deadline + difficulty + estimated hours + overdue penalty) and uses GPT to draft a realistic weekly plan around the hours I actually have.

## Features

- **Auth** — JWT-based signup/login (Spring Security)
- **Task manager** — CRUD with course, due date, estimated hours, difficulty, status
- **Kanban board** — drag tasks between Backlog → This Week → Today → In Progress → Done
- **Smart Priority Score** — custom algorithm that ranks tasks (see `PriorityService.java`)
- **AI Weekly Study Plan** — sends your open tasks + available hours to GPT-4o-mini, gets back a day-by-day plan
- **Pomodoro focus timer** — logs sessions against tasks
- **Analytics** — hours studied, completion rate, overdue trend (Recharts)
- **Workload risk warning** — flags overloaded weeks before they happen

## Tech Stack

**Backend:** Java 17, Spring Boot 3, Spring Security, JWT, PostgreSQL, JPA/Hibernate
**Frontend:** React 18, TypeScript, TailwindCSS, Vite, Recharts, @hello-pangea/dnd
**AI:** OpenAI API (gpt-4o-mini)

## Architecture

```
React (Vite) ──HTTP──▶ Spring Boot REST API ──JPA──▶ PostgreSQL
                                │
                                └──HTTPS──▶ OpenAI API
```

See `docs/architecture.md` for the full diagram.

## Running locally

### Prerequisites
- Java 17+
- Node 18+
- PostgreSQL 14+
- An OpenAI API key

### Backend
```bash
cd backend
# create the db
createdb studyos
# set env vars (or edit application.properties)
export DB_URL=jdbc:postgresql://localhost:5432/studyos
export DB_USER=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=change-me-to-something-long-and-random-at-least-32-chars
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run
```
Backend runs on `http://localhost:8080`.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`.

## API

See `docs/api.md`. Highlights:

```
POST   /api/auth/register
POST   /api/auth/login
GET    /api/tasks
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
GET    /api/dashboard
GET    /api/analytics/weekly
POST   /api/ai/study-plan
POST   /api/focus-sessions
```

## What I'd do next

- Deploy to Railway / Vercel (currently runs locally)
- Add Google OAuth
- WebSocket-based live updates for the Kanban (multi-device)
- Caching with Redis for the dashboard query
- A mobile-friendly polish pass

## Things I learned the hard way

- JWT secret rotation is annoying. I ended up putting it in env vars on day 2 after committing a secret to git (then rewriting history with `git filter-repo`).
- `@Transactional` placement matters more than I thought. Lazy loading exceptions taught me that.
- Tailwind + drag-and-drop needed `select-none` on the cards or text highlighting broke the drag.
- Prompting GPT to return strict JSON is way harder than the docs make it sound; I had to add a retry-on-parse-failure.

---

Built by [Your Name] — feel free to open issues or PRs.
