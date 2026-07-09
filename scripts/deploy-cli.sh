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

read_env_value() {
  local name="${1:?name is required}"
  local default_value="${2:-}"
  local value
  value="$(grep -E "^${name}=" "$ROOT/.env" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d ' \r' || true)"
  echo "${value:-$default_value}"
}

assert_dev_ports_available() {
  local compose_project_name="${1:?compose project is required}"
  local ports rows conflicts=()

  ports=(
    "$(read_env_value FRONTEND_PORT 5173)"
    "$(read_env_value BACKEND_PORT 8080)"
    "$(read_env_value STORAGE_MYSQL_PORT 3307)"
    "$(read_env_value STORAGE_MINIO_PORT 9000)"
  )
  rows="$(docker ps --format '{{.Names}}|{{.Ports}}' 2>/dev/null || true)"
  [[ -n "$rows" ]] || return 0

  local row name port_text port
  while IFS= read -r row; do
    [[ "$row" == *"|"* ]] || continue
    name="${row%%|*}"
    port_text="${row#*|}"
    [[ "$name" == "$compose_project_name"-* ]] && continue
    for port in "${ports[@]}"; do
      if [[ "$port_text" == *":${port}->"* ]]; then
        conflicts+=("  port ${port} is already used by container ${name} (${port_text})")
      fi
    done
  done <<< "$rows"

  if [[ "${#conflicts[@]}" -gt 0 ]]; then
    echo "Cannot start dev profile because required ports are already in use by another Docker project." >&2
    echo "Stop the conflicting project first, for example: docker compose -p <project-name> down" >&2
    echo "Conflicts:" >&2
    printf '%s\n' "${conflicts[@]}" | sort -u >&2
    exit 1
  fi
}

cd "$ROOT"
write_worktree_env_file "$ROOT" >/dev/null

compose_files=(-f docker-compose.yml)
if [[ "$PROFILE" == "dev" ]]; then
  compose_files+=(-f docker-compose-dev.yml)
  assert_dev_ports_available "$COMPOSE_PROJECT_NAME"
fi

echo "Deploy profile: $PROFILE"
docker compose --env-file "$ROOT/.env" "${compose_files[@]}" up -d ${BUILD_FLAG}

if [[ "$OPEN_BROWSER" -eq 1 ]]; then
  if [[ "$PROFILE" == "dev" ]]; then
    port="$(read_env_value FRONTEND_PORT 5173)"
  else
    port="$(read_env_value APP_PORT 80)"
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
