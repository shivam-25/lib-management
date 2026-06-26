#!/usr/bin/env bash
# Runs once, after the container is created (postCreateCommand).
# Installs the psql client, seeds the database, installs deps, builds the Java service.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Installing PostgreSQL client"
sudo apt-get update -y
sudo apt-get install -y postgresql-client

echo "==> Waiting for Postgres (db:5432)"
export PGPASSWORD=postgres
until pg_isready -h db -U postgres >/dev/null 2>&1; do sleep 1; done

echo "==> Seeding database (if empty)"
if [ "$(psql -h db -U postgres -d school_mgmt -tAc "SELECT to_regclass('public.users') IS NOT NULL;")" = "t" ]; then
  echo "    users table already exists -> skipping seed"
else
  psql -h db -U postgres -d school_mgmt -v ON_ERROR_STOP=1 -f seed_db/tables.sql
  psql -h db -U postgres -d school_mgmt -v ON_ERROR_STOP=1 -f seed_db/seed-db.sql
  echo "    seed complete"
fi

echo "==> Installing backend dependencies"
( cd backend && npm install )

echo "==> Installing frontend dependencies"
( cd frontend && npm install )

echo "==> Building Java report service"
( cd Java-service && mvn -q -DskipTests package ) || echo "    (Java build failed/skipped — service is optional for the demo)"

echo "==> Setup complete"
