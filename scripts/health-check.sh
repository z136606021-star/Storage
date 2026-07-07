#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_PORT=8080
FRONTEND_PORT=5173
failures=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-root)
      ROOT="$(cd "${2:?--repo-root requires a path}" && pwd)"
      shift 2
      ;;
    --backend-port)
      BACKEND_PORT="${2:?--backend-port requires a port}"
      shift 2
      ;;
    --frontend-port)
      FRONTEND_PORT="${2:?--frontend-port requires a port}"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2
      ;;
  esac
done

# shellcheck source=worktree-db.sh
source "$SCRIPT_DIR/worktree-db.sh"

check_result() {
  local name="${1:?name is required}"
  local ok="${2:?ok is required}"
  local detail="${3-}"
  if [[ "$ok" == true ]]; then
    echo "[OK]   $name - $detail"
  else
    echo "[FAIL] $name - $detail"
    failures=$((failures + 1))
  fi
}

container_running() {
  local name="${1:?container name is required}"
  [[ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null || true)" == "true" ]]
}

port_listening() {
  local port="${1:?port is required}"
  if command -v lsof >/dev/null 2>&1; then
    [[ -n "$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1)" ]]
    return
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "sport = :$port" 2>/dev/null | grep -q ":$port"
    return
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z localhost "$port" >/dev/null 2>&1
    return
  fi
  return 1
}

echo "Storage dev health check"
echo "Project root: $ROOT"
echo

if load_current_worktree_profile "$ROOT"; then
  check_result branch true "branch=$WORKTREE_BRANCH"
  PROFILE_MYSQL_CONTAINER="$STORAGE_MYSQL_CONTAINER"
  PROFILE_MINIO_CONTAINER="$STORAGE_MINIO_CONTAINER"
  PROFILE_MYSQL_PORT="$STORAGE_MYSQL_PORT"
  normalized_root="$(cd "$ROOT" && pwd -P)"
  if [[ "$WORKTREE_PATH" == /* ]]; then
    [[ "$normalized_root" == "$WORKTREE_PATH" ]] && path_ok=true || path_ok=false
    check_result worktree-path "$path_ok" "expected=$WORKTREE_PATH actual=$normalized_root"
  else
    check_result worktree-path true "actual=$normalized_root"
  fi
else
  check_result branch false "unknown branch or detached HEAD"
  PROFILE_MYSQL_CONTAINER=""
  PROFILE_MINIO_CONTAINER=""
  PROFILE_MYSQL_PORT=""
fi

if [[ -f "$ROOT/.env" ]]; then
  import_worktree_env_file "$ROOT"
  BACKEND_PORT="${BACKEND_PORT:-8080}"
  FRONTEND_PORT="${FRONTEND_PORT:-5173}"
  if [[ "${MYSQL_PORT:-}" == "$PROFILE_MYSQL_PORT" && "${STORAGE_MYSQL_CONTAINER:-}" == "$PROFILE_MYSQL_CONTAINER" ]]; then
    check_result .env true "MYSQL_PORT=${MYSQL_PORT:-} container=${STORAGE_MYSQL_CONTAINER:-}"
  else
    check_result .env false "MYSQL_PORT=${MYSQL_PORT:-missing} container=${STORAGE_MYSQL_CONTAINER:-missing}"
  fi
else
  check_result .env false "missing; run sync-worktree-env.sh"
fi

if [[ -n "$PROFILE_MYSQL_CONTAINER" ]] && container_running "$PROFILE_MYSQL_CONTAINER"; then
  check_result mysql-container true "$PROFILE_MYSQL_CONTAINER"
  mysql_running=true
else
  check_result mysql-container false "${PROFILE_MYSQL_CONTAINER:-no profile}"
  mysql_running=false
fi

if [[ -n "$PROFILE_MINIO_CONTAINER" ]] && container_running "$PROFILE_MINIO_CONTAINER"; then
  check_result minio-container true "$PROFILE_MINIO_CONTAINER"
else
  check_result minio-container false "${PROFILE_MINIO_CONTAINER:-no profile}"
fi

legacy_mysql="$(docker ps -a --filter "name=^/material-ledger-mysql$" --format "{{.Names}}" 2>/dev/null || true)"
legacy_minio="$(docker ps -a --filter "name=^/material-ledger-minio$" --format "{{.Names}}" 2>/dev/null || true)"
if [[ -z "$legacy_mysql" && -z "$legacy_minio" ]]; then
  check_result legacy-docker true "no material-ledger-* containers"
else
  check_result legacy-docker false "run cleanup-legacy-docker.sh (material-ledger-* still exists)"
fi

if [[ "$mysql_running" == true ]]; then
  MYSQL_USER="${MYSQL_USER:-storage}"
  MYSQL_PASSWORD="${MYSQL_PASSWORD:-storage123}"
  MYSQL_DB="${MYSQL_DB:-storage}"
  sample="$(docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" "$PROFILE_MYSQL_CONTAINER" \
    mysql "-u$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DB" -N \
    -e "SELECT CONCAT(category, '|', name) FROM material_ledger LIMIT 1;" 2>/dev/null || true)"
  if [[ -n "$sample" && "$sample" != *"?"* ]]; then
    check_result mysql-chinese true "sample=$sample"
  else
    check_result mysql-chinese false "sample=$sample"
  fi
else
  check_result mysql-chinese false "mysql container not running"
fi

backend_status="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$BACKEND_PORT/api/auth/me" || true)"
if [[ "$backend_status" == "200" || "$backend_status" == "401" || "$backend_status" == "403" ]]; then
  check_result backend true "http://localhost:$BACKEND_PORT"
else
  check_result backend false "http://localhost:$BACKEND_PORT"
fi

if port_listening "$FRONTEND_PORT"; then
  check_result frontend true "http://localhost:$FRONTEND_PORT"
else
  check_result frontend false "http://localhost:$FRONTEND_PORT"
fi

echo
if (( failures == 0 )); then
  echo "All checks passed."
  exit 0
fi

echo "$failures check(s) failed."
exit 1
