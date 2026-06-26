#!/usr/bin/env bash
# Runs on every container start (postStartCommand).
# Generates env files wired for the single-origin Codespaces URL, then starts all services.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# --- Resolve the public app URL (single origin = the frontend's port 5173) ---
if [ -n "${CODESPACE_NAME:-}" ] && [ -n "${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-}" ]; then
  APP_URL="https://${CODESPACE_NAME}-5173.${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}"
else
  APP_URL="http://localhost:5173"
fi
echo "==> App URL: $APP_URL"

# --- Backend .env ---
# Single-origin fix: COOKIE_DOMAIN is intentionally OMITTED so auth cookies are
# host-only and bind to the *.app.github.dev host (lets js-cookie read csrfToken).
cat > backend/.env <<EOF
PORT=5007
DATABASE_URL=postgres://postgres:postgres@db:5432/school_mgmt
JWT_ACCESS_TOKEN_SECRET=dev_access_secret_change_me
JWT_REFRESH_TOKEN_SECRET=dev_refresh_secret_change_me
CSRF_TOKEN_SECRET=dev_csrf_secret_change_me
JWT_ACCESS_TOKEN_TIME_IN_MS=900000
JWT_REFRESH_TOKEN_TIME_IN_MS=28800000
CSRF_TOKEN_TIME_IN_MS=950000
EMAIL_VERIFICATION_TOKEN_SECRET=dev_email_secret_change_me
EMAIL_VERIFICATION_TOKEN_TIME_IN_MS=18000000
PASSWORD_SETUP_TOKEN_SECRET=dev_pwd_secret_change_me
PASSWORD_SETUP_TOKEN_TIME_IN_MS=300000
MAIL_FROM_USER=noreply@school-admin.com
RESEND_API_KEY=re_dummy_key
UI_URL=${APP_URL}
API_URL=${APP_URL}
NODE_ENV=development
EOF

# --- Frontend .env ---
# Empty VITE_API_URL -> the app calls relative /api/v1, which the Vite proxy
# forwards to the backend on the SAME origin (no cross-site cookies).
cat > frontend/.env <<EOF
VITE_API_URL=
VITE_APP_NAME=Student Management System
VITE_APP_VERSION=1.0.0
EOF

mkdir -p /tmp/logs

echo "==> (Re)starting services"
pkill -f "src/server.js" 2>/dev/null || true
pkill -f "vite" 2>/dev/null || true
pkill -f "report-.*\.jar" 2>/dev/null || true

# Backend (Express, :5007)
( cd backend && nohup npm start > /tmp/logs/backend.log 2>&1 & )

# Frontend (Vite, :5173 — proxies /api -> :5007)
( cd frontend && nohup npm run dev -- --host 0.0.0.0 --port 5173 > /tmp/logs/frontend.log 2>&1 & )

# Java report service (:5008) — optional, only if the jar built
JAR="$(ls Java-service/target/*.jar 2>/dev/null | head -n1 || true)"
if [ -n "$JAR" ]; then
  ( cd Java-service && SERVER_PORT=5008 BACKEND_BASE_URL=http://localhost:5007/api/v1 \
      nohup java -jar "target/$(basename "$JAR")" > /tmp/logs/java.log 2>&1 & )
fi

echo "==> All services launching. Logs: /tmp/logs/{backend,frontend,java}.log"
echo "==> Open port 5173 (Ports tab) to use the app. Login: admin@school-admin.com / 3OU4zn3q6Zh9"
