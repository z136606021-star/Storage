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
  local ports rows conflicts=()

  ports=(
    "$(read_env_value FRONTEND_PORT 5173)"
    "$(read_env_value BACKEND_PORT 8080)"
    "3307"
    "9000"
  )
  rows="$(docker ps --format '{{.Names}}|{{.Ports}}' 2>/dev/null || true)"
  [[ -n "$rows" ]] || return 0

  local row name port_text port
  while IFS= read -r row; do
    [[ "$row" == *"|"* ]] || continue
    name="${row%%|*}"
    port_text="${row#*|}"
    [[ "$name" == storage-* ]] && continue
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

read_deploy_port() {
  if [[ "$PROFILE" == "dev" ]]; then
    read_env_value FRONTEND_PORT 5173
  else
    read_env_value APP_PORT 80
  fi
}

wait_http_endpoint() {
  local url="${1:?url is required}"
  local name="${2:?name is required}"
  local timeout_seconds="${3:-90}"
  local deadline=$((SECONDS + timeout_seconds))

  echo "Waiting for ${name} at ${url} ..."
  while (( SECONDS < deadline )); do
    if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
      echo "${name} is ready."
      return 0
    fi
    sleep 2
  done

  echo "${name} did not become ready within ${timeout_seconds} seconds: ${url}" >&2
  return 1
}

wait_deploy_ready() {
  if [[ "$PROFILE" == "dev" ]]; then
    backend_port="$(read_env_value BACKEND_PORT 8080)"
    wait_http_endpoint "http://localhost:${backend_port}/health" backend
  fi

  port="$(read_deploy_port)"
  wait_http_endpoint "http://localhost:${port}" frontend
}

assert_prod_env_ready() {
  local env_path="$ROOT/.env"
  if [[ ! -f "$env_path" ]]; then
    echo "Production deploy requires an existing .env at $env_path." >&2
    echo "Create it from .env.example and set external MYSQL_* and MINIO_* connection settings." >&2
    echo "Production does not auto-generate .env; use sync-worktree-env only for local dev." >&2
    exit 1
  fi

  local name value missing=()
  for name in MYSQL_HOST MYSQL_PORT MYSQL_DB MYSQL_USER MYSQL_PASSWORD MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY MINIO_BUCKET; do
    value="$(read_env_value "$name" "")"
    if [[ -z "$value" ]]; then
      missing+=("$name")
    fi
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "Production .env is missing required connection settings: ${missing[*]}." >&2
    echo "Configure external MySQL and MinIO in .env before deploying prod." >&2
    exit 1
  fi
}

cd "$ROOT"
if [[ "$PROFILE" == "dev" ]]; then
  write_worktree_env_file "$ROOT" >/dev/null
else
  assert_prod_env_ready
fi

compose_files=(-f docker-compose.yml)
if [[ "$PROFILE" == "dev" ]]; then
  compose_files+=(-f docker-compose-dev.yml)
  assert_dev_ports_available
fi

echo "Deploy profile: $PROFILE"
docker compose --env-file "$ROOT/.env" "${compose_files[@]}" up -d ${BUILD_FLAG}
wait_deploy_ready

if [[ "$OPEN_BROWSER" -eq 1 ]]; then
  port="$(read_deploy_port)"
  url="http://localhost:${port}"
  echo "Opening ${url} ..."
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 || true
  elif command -v open >/dev/null 2>&1; then
    open "$url" >/dev/null 2>&1 || true
  fi
fi

echo "Done. Services are running in background."
