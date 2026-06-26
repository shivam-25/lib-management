# Codespaces dev container

One-click cloud environment for the full stack: PostgreSQL + Node backend + Vite
frontend + Java/Maven report service, with the **single-origin proxy fix** baked in
so login works on the `*.app.github.dev` URL.

## How to launch

1. Push this branch to GitHub (already done if you're reading this on `main`).
2. On the repo page: **`< > Code` ▸ Codespaces ▸ Create codespace on main**.
3. Wait for the build (~5–10 min the first time): it provisions Postgres, Node 22,
   Java 21 + Maven, seeds the DB, installs deps, builds the Java jar, and starts
   every service.
4. When it's up, open the **Ports** tab and click the 🌐 globe / "Open in Browser"
   next to **5173 (Frontend)**. The app opens at `https://<name>-5173.app.github.dev`.

## Log in

| Field    | Value                       |
|----------|-----------------------------|
| Email    | `admin@school-admin.com`    |
| Password | `3OU4zn3q6Zh9`              |

## Sharing the URL (e.g. with another tool/reviewer)

Forwarded ports are **Private** by default — only you (the Codespace owner) can open
them, which is all you need to record a demo in your own browser.

To let *someone else* hit it: in the **Ports** tab, right-click port **5173 ▸ Port
Visibility ▸ Public**, then share the URL. Only 5173 needs to be public — the backend
(5007) and Java service (5008) are reached internally via the proxy.

## What runs where

| Service            | Port | Notes                                                        |
|--------------------|------|--------------------------------------------------------------|
| Frontend (Vite)    | 5173 | **public entry point**; proxies `/api` → backend             |
| Backend (Express)  | 5007 | internal; reached via the Vite proxy (same origin)           |
| Java report svc    | 5008 | optional; `GET /api/v1/students/{id}/report` → PDF           |
| PostgreSQL         | 5432 | `db` service in docker-compose; seeded on first create       |

## Restarting services manually

```bash
bash .devcontainer/start.sh          # regenerates env + restarts everything
tail -f /tmp/logs/backend.log        # backend logs
tail -f /tmp/logs/frontend.log       # frontend logs
tail -f /tmp/logs/java.log           # java logs
```
