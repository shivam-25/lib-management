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

echo "==> Ensuring demo class/section data (required to add a student)"
psql -h db -U postgres -d school_mgmt -c "INSERT INTO classes(name,sections) VALUES ('Grade 5','A,B'),('Grade 6','A,B') ON CONFLICT DO NOTHING;"
psql -h db -U postgres -d school_mgmt -c "INSERT INTO sections(name) VALUES ('A'),('B') ON CONFLICT DO NOTHING;"
psql -h db -U postgres -d school_mgmt -c "SELECT student_add_update('{\"name\":\"Alice Johnson\",\"email\":\"alice.johnson@demo.test\",\"gender\":\"Female\",\"phone\":\"0123456789\",\"dob\":\"2008-04-12\",\"class\":\"Grade 5\",\"section\":\"A\",\"roll\":1,\"admissionDate\":\"2020-01-10\",\"fatherName\":\"Robert Johnson\",\"systemAccess\":true}'::jsonb);" >/dev/null 2>&1 || true

echo "==> Installing backend dependencies"
( cd backend && npm install )

echo "==> Installing frontend dependencies"
( cd frontend && npm install --ignore-scripts )   # skip husky 'prepare' (fails in the nested-git layout)

echo "==> Building Java report service"
( cd Java-service && mvn -q -DskipTests package ) || echo "    (Java build failed/skipped — service is optional for the demo)"

echo "==> Setup complete"
