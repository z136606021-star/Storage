#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROFILE_NAME="dev"
BACKEND_PORT=8080
FRONTEND_PORT=5173
failures=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      PROFILE_NAME="${2:?--profile requires dev or prod}"
      shift 2
      ;;
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

if [[ "$PROFILE_NAME" != "dev" && "$PROFILE_NAME" != "prod" ]]; then
  echo "--profile must be dev or prod" >&2
  exit 2
fi

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

echo "Storage health check (profile: $PROFILE_NAME)"
echo "Project root: $ROOT"
echo

DEV_MYSQL_CONTAINER='storage-mysql'
DEV_MINIO_CONTAINER='storage-minio'
DEV_BACKEND_CONTAINER='storage-backend'
DEV_MINIO_HOST_PORT=9000

if load_current_worktree_profile "$ROOT"; then
  check_result branch true "branch=$WORKTREE_BRANCH"
  normalized_root="$(cd "$ROOT" && pwd -P)"
  if [[ "$WORKTREE_PATH" == /* ]]; then
    [[ "$normalized_root" == "$WORKTREE_PATH" ]] && path_ok=true || path_ok=false
    check_result worktree-path "$path_ok" "expected=$WORKTREE_PATH actual=$normalized_root"
  else
    check_result worktree-path true "actual=$normalized_root"
  fi
else
  check_result branch false "unknown branch or detached HEAD"
fi

if [[ -f "$ROOT/.env" ]]; then
  import_worktree_env_file "$ROOT"
  BACKEND_PORT="${BACKEND_PORT:-8080}"
  if [[ "$PROFILE_NAME" == "prod" ]]; then
    FRONTEND_PORT="${APP_PORT:-80}"
  else
    FRONTEND_PORT="${FRONTEND_PORT:-5173}"
  fi
  check_result .env true "found at $ROOT/.env"
else
  check_result .env false "missing; run sync-worktree-env.sh for local dev or create prod .env manually"
fi

if [[ "$PROFILE_NAME" == "dev" ]]; then
  if container_running "$DEV_MYSQL_CONTAINER"; then
    check_result mysql-container true "$DEV_MYSQL_CONTAINER"
    mysql_running=true
  else
    check_result mysql-container false "$DEV_MYSQL_CONTAINER"
    mysql_running=false
  fi
  if container_running "$DEV_MINIO_CONTAINER"; then
    check_result minio-container true "$DEV_MINIO_CONTAINER"
    minio_running=true
  else
    check_result minio-container false "$DEV_MINIO_CONTAINER"
    minio_running=false
  fi
else
  check_result mysql-container true "skipped (prod uses external MySQL)"
  check_result minio-container true "skipped (prod uses external MinIO)"
  mysql_running=false
  minio_running=false
fi

if container_running "$DEV_BACKEND_CONTAINER"; then
  check_result backend-container true "$DEV_BACKEND_CONTAINER"
  backend_running=true
else
  check_result backend-container false "$DEV_BACKEND_CONTAINER"
  backend_running=false
fi

legacy_mysql="$(docker ps -a --filter "name=^/material-ledger-mysql$" --format "{{.Names}}" 2>/dev/null || true)"
legacy_minio="$(docker ps -a --filter "name=^/material-ledger-minio$" --format "{{.Names}}" 2>/dev/null || true)"
if [[ -z "$legacy_mysql" && -z "$legacy_minio" ]]; then
  check_result legacy-docker true "no material-ledger-* containers"
else
  check_result legacy-docker false "run cleanup-legacy-docker.sh (material-ledger-* still exists)"
fi

if [[ "$PROFILE_NAME" == "dev" && "$mysql_running" == true ]]; then
  MYSQL_USER="${MYSQL_USER:-storage}"
  MYSQL_PASSWORD="${MYSQL_PASSWORD:-storage123}"
  MYSQL_DB="${MYSQL_DB:-storage}"
  sample="$(docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" "$DEV_MYSQL_CONTAINER" \
    mysql "-u$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DB" -N \
    -e "SELECT name FROM sys_menu WHERE id = 111 LIMIT 1;" 2>/dev/null || true)"
  if [[ -n "$sample" && "$sample" != *"?"* ]]; then
    check_result mysql-chinese true "sample=$sample"
  else
    check_result mysql-chinese false "sample=$sample"
  fi
elif [[ "$PROFILE_NAME" == "dev" ]]; then
  check_result mysql-chinese false "mysql container not running"
elif [[ "$backend_running" == true ]]; then
  mysql_host="$(docker exec "$DEV_BACKEND_CONTAINER" printenv MYSQL_HOST 2>/dev/null || true)"
  mysql_port="$(docker exec "$DEV_BACKEND_CONTAINER" printenv MYSQL_PORT 2>/dev/null || true)"
  if [[ -n "$mysql_host" && -n "$mysql_port" ]]; then
    check_result mysql-endpoint true "MYSQL_HOST=$mysql_host MYSQL_PORT=$mysql_port"
  else
    check_result mysql-endpoint false "MYSQL_HOST/MYSQL_PORT missing"
  fi
else
  check_result mysql-endpoint false "backend container not running"
fi

if [[ "$PROFILE_NAME" == "dev" ]]; then
  if [[ "$minio_running" == true && "$backend_running" == true ]]; then
    backend_key="$(docker exec "$DEV_BACKEND_CONTAINER" printenv MINIO_ACCESS_KEY 2>/dev/null || true)"
    minio_key="$(docker exec "$DEV_MINIO_CONTAINER" printenv MINIO_ROOT_USER 2>/dev/null || true)"
    if [[ -n "$backend_key" && "$backend_key" == "$minio_key" ]]; then
      check_result minio-credentials true "backend=$backend_key minio=$minio_key"
    else
      check_result minio-credentials false "backend=${backend_key:-missing} minio=${minio_key:-missing}"
    fi

    if curl -fsS "http://127.0.0.1:$DEV_MINIO_HOST_PORT/minio/health/live" >/dev/null 2>&1; then
      check_result minio-live true "http://127.0.0.1:$DEV_MINIO_HOST_PORT/minio/health/live"
    else
      check_result minio-live false "http://127.0.0.1:$DEV_MINIO_HOST_PORT/minio/health/live"
    fi

    if docker exec "$DEV_BACKEND_CONTAINER" curl -fsS "http://minio:9000/minio/health/live" >/dev/null 2>&1; then
      check_result minio-from-backend true "http://minio:9000/minio/health/live"
    else
      check_result minio-from-backend false "http://minio:9000/minio/health/live"
    fi
  else
    check_result minio-credentials false "backend or minio container not running"
    check_result minio-live false "minio container not running"
    check_result minio-from-backend false "backend or minio container not running"
  fi
elif [[ "$backend_running" == true ]]; then
  minio_endpoint="$(docker exec "$DEV_BACKEND_CONTAINER" printenv MINIO_ENDPOINT 2>/dev/null || true)"
  if [[ -n "$minio_endpoint" ]]; then
    check_result minio-endpoint true "MINIO_ENDPOINT=$minio_endpoint"
  else
    check_result minio-endpoint false "MINIO_ENDPOINT missing"
  fi

  if [[ -n "$minio_endpoint" ]] && docker exec "$DEV_BACKEND_CONTAINER" curl -fsS "${minio_endpoint%/}/minio/health/live" >/dev/null 2>&1; then
    check_result minio-from-backend true "$minio_endpoint"
  else
    check_result minio-from-backend false "${minio_endpoint:-backend container not configured}"
  fi
else
  check_result minio-endpoint false "backend container not running"
  check_result minio-from-backend false "backend container not running"
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
