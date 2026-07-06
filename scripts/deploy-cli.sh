#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROFILE="dev"
BUILD_FLAG=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      PROFILE="${2:-}"
      shift 2
      ;;
    --build)
      BUILD_FLAG="--build"
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2
      ;;
  esac
done

if [[ "$PROFILE" != "dev" && "$PROFILE" != "prod" ]]; then
  echo "--profile must be dev or prod" >&2
  exit 2
fi

# shellcheck source=worktree-db.sh
source "$SCRIPT_DIR/worktree-db.sh"

cd "$ROOT"
write_worktree_env_file "$ROOT" >/dev/null

compose_files=(-f docker-compose.yml)
if [[ "$PROFILE" == "dev" ]]; then
  compose_files+=(-f docker-compose-dev.yml)
fi

echo "Deploy profile: $PROFILE"
docker compose --env-file "$ROOT/.env" "${compose_files[@]}" up -d ${BUILD_FLAG}
echo "Done. Services are running in background."
