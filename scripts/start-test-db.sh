#!/usr/bin/env bash
set -euo pipefail
NAME=jobsearcher-test-pg
if docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  docker start "$NAME" >/dev/null
else
  docker run -d --name "$NAME" \
    -e POSTGRES_DB=jobsearcher \
    -e POSTGRES_USER=jobsearcher \
    -e POSTGRES_PASSWORD=jobsearcher \
    -p 55432:5432 \
    postgres:16-alpine
fi
for i in $(seq 1 30); do
  if docker exec "$NAME" pg_isready -U jobsearcher -d jobsearcher >/dev/null 2>&1; then
    echo "Test Postgres ready on localhost:55432"
    exit 0
  fi
  sleep 1
done
echo "Postgres failed to become ready" >&2
exit 1
