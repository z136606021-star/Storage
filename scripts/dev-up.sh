#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESET_DB=false
SKIP_DOCKER=false
INSTALL=false
NO_KILL=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --reset-db)
      RESET_DB=true
      shift
      ;;
    --skip-docker)
      SKIP_DOCKER=true
      shift
      ;;
    --install)
      INSTALL=true
      shift
      ;;
    --no-kill)
      NO_KILL=true
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2
      ;;
  esac
done

# shellcheck source=worktree-db.sh
source "$SCRIPT_DIR/worktree-db.sh"

cd "$ROOT"
write_worktree_env_file "$ROOT" >/dev/null
load_current_worktree_profile "$ROOT"

cat <<EOF
Storage dev-up
  Branch: $WORKTREE_BRANCH
  MySQL:  localhost:$STORAGE_MYSQL_PORT

EOF

if [[ "$RESET_DB" == true ]]; then
  echo "Resetting database..."
  "$SCRIPT_DIR/reset-db.sh"
  echo
elif [[ "$SKIP_DOCKER" == false ]]; then
  echo "Starting Docker (MySQL + MinIO)..."
  docker compose --env-file "$ROOT/.env" up -d
  "$SCRIPT_DIR/wait-mysql.sh" --require-seed-data --repo-root "$ROOT"
  echo
fi

start_args=()
[[ "$INSTALL" == true ]] && start_args+=(--install)
[[ "$NO_KILL" == true ]] && start_args+=(--no-kill)
"$SCRIPT_DIR/start-dev.sh" "${start_args[@]}"
