#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROFILE="dev"
BUILD_FLAG=""
OPEN_BROWSER=1

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
    --no-open-browser)
      OPEN_BROWSER=0
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

if [[ "$OPEN_BROWSER" -eq 1 ]]; then
  if [[ "$PROFILE" == "dev" ]]; then
    port="$(grep -E '^FRONTEND_PORT=' "$ROOT/.env" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d ' \r' || true)"
    port="${port:-5173}"
  else
    port="$(grep -E '^APP_PORT=' "$ROOT/.env" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d ' \r' || true)"
    port="${port:-80}"
  fi
  url="http://localhost:${port}"
  echo "Opening ${url} ..."
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 || true
  elif command -v open >/dev/null 2>&1; then
    open "$url" >/dev/null 2>&1 || true
  fi
fi

echo "Done. Services are running in background."
