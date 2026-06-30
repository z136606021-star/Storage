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
Worktree DB profile synced.
  Branch:   $WORKTREE_BRANCH
  Path:     $ROOT
  MySQL:    localhost:$STORAGE_MYSQL_PORT (container: $STORAGE_MYSQL_CONTAINER)
  MinIO:    http://localhost:$STORAGE_MINIO_PORT (console: $STORAGE_MINIO_CONSOLE_PORT)
  Compose:  $COMPOSE_PROJECT_NAME
  Wrote:    $env_path

Next: docker compose --env-file .env up -d
EOF
