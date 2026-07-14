#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# shellcheck source=worktree-db.sh
source "$SCRIPT_DIR/worktree-db.sh"

cd "$ROOT"
write_worktree_env_file "$ROOT" >/dev/null
load_current_worktree_profile "$ROOT"
env_path="$ROOT/.env"

cat <<EOF
Worktree env synced (dynamic settings only).
  Branch:   $WORKTREE_BRANCH
  Path:     $ROOT
  MySQL:    localhost:3307 (container: storage-mysql)
  MinIO:    http://localhost:9000 (container: storage-minio)
  Wrote:    $env_path

Next (dev): docker compose --env-file .env -f docker-compose.yml -f docker-compose-dev.yml up -d
Production uses a manually maintained .env with external MYSQL_* and MINIO_*; do not run sync-worktree-env for prod.
EOF
