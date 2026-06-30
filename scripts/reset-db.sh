#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# shellcheck source=worktree-db.sh
source "$SCRIPT_DIR/worktree-db.sh"

cd "$ROOT"
write_worktree_env_file "$ROOT" >/dev/null
import_worktree_env_file "$ROOT"
load_current_worktree_profile "$ROOT"

MYSQL_USER="${MYSQL_USER:-storage}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-storage123}"
MYSQL_DB="${MYSQL_DB:-storage}"

cat <<EOF
Resetting database for branch: $WORKTREE_BRANCH
  MySQL port: $STORAGE_MYSQL_PORT
  Container:  $STORAGE_MYSQL_CONTAINER
  Volume:     $STORAGE_MYSQL_VOLUME

EOF

echo "Stopping containers and removing volumes..."
docker compose --env-file "$ROOT/.env" down -v

echo "Starting fresh MySQL + MinIO..."
docker compose --env-file "$ROOT/.env" up -d

echo "Waiting for MySQL to initialize..."
"$SCRIPT_DIR/wait-mysql.sh" --require-seed-data --repo-root "$ROOT"

count="$(docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" "$STORAGE_MYSQL_CONTAINER" \
  mysql "-u$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DB" -N -e "SELECT COUNT(*) FROM material_ledger;" 2>/dev/null || true)"
echo "material_ledger rows: $count"

docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" "$STORAGE_MYSQL_CONTAINER" \
  mysql "-u$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DB" \
  -e "SELECT id, category, name FROM material_ledger LIMIT 3;" 2>/dev/null || true

echo "Done. Restart backend: ./scripts/start-dev.sh"
