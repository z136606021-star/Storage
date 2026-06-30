#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT/backend"
FRONTEND_DIR="$ROOT/frontend"
INSTALL=false
WITH_DOCKER=false
NO_KILL=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --install)
      INSTALL=true
      shift
      ;;
    --with-docker)
      WITH_DOCKER=true
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
import_worktree_env_file "$ROOT"
load_current_worktree_profile "$ROOT"

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

port_pid() {
  local port="${1:?port is required}"
  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1
    return
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :$port" 2>/dev/null | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' | head -n 1
    return
  fi
  return 0
}

port_in_use() {
  [[ -n "$(port_pid "$1")" ]]
}

process_cmd() {
  local pid="${1:?pid is required}"
  if [[ -r "/proc/$pid/cmdline" ]]; then
    tr '\0' ' ' < "/proc/$pid/cmdline"
  else
    ps -p "$pid" -o command= 2>/dev/null || true
  fi
}

resolve_port_conflict() {
  local port="${1:?port is required}"
  local service_name="${2:?service name is required}"
  local pattern="${3:?pattern is required}"
  local pid
  pid="$(port_pid "$port")"
  [[ -z "$pid" ]] && return

  local cmd
  cmd="$(process_cmd "$pid")"
  if [[ "$cmd" =~ $pattern ]]; then
    if [[ "$NO_KILL" == true ]]; then
      echo "Port $port is in use by expected $service_name process (PID $pid). Re-run without --no-kill to auto-restart it." >&2
      return
    fi
    echo "Port $port in use by existing $service_name (PID $pid), stopping..."
    kill "$pid" 2>/dev/null || true
    sleep 1
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
    return
  fi

  echo "Port $port is occupied by PID $pid: $cmd" >&2
  echo "This does not look like the Storage dev $service_name. Stop that process manually, or change its port." >&2
  exit 1
}

wait_backend_ready() {
  local port="${1:?port is required}"
  local timeout="${2:-120}"
  local deadline=$((SECONDS + timeout))
  local status

  echo "Waiting for backend on http://localhost:$port ..."
  while (( SECONDS < deadline )); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/api/auth/me" || true)"
    if [[ "$status" == "200" || "$status" == "401" || "$status" == "403" ]]; then
      echo "Backend is ready."
      return 0
    fi
    sleep 2
  done
  return 1
}

cleanup() {
  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo
    echo "Stopping frontend (PID $FRONTEND_PID)..."
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "Stopping backend (PID $BACKEND_PID)..."
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

cat <<EOF
Storage dev launcher
Project root: $ROOT
Branch:       $WORKTREE_BRANCH
MySQL:        localhost:$STORAGE_MYSQL_PORT ($STORAGE_MYSQL_CONTAINER)

EOF

if [[ "$WITH_DOCKER" == true ]]; then
  echo "Starting Docker (MySQL + MinIO)..."
  docker compose --env-file "$ROOT/.env" up -d
  "$SCRIPT_DIR/wait-mysql.sh" --require-seed-data --repo-root "$ROOT"
  echo "Docker started."
  echo
fi

echo "Checking ports..."
resolve_port_conflict "$BACKEND_PORT" backend "($ROOT|$BACKEND_DIR|spring-boot:run|storage-backend|com\.storage)"
resolve_port_conflict "$FRONTEND_PORT" frontend "($ROOT|$FRONTEND_DIR|vite|npm run dev|node_modules/.*/vite)"

if ! port_in_use "$STORAGE_MYSQL_PORT"; then
  echo "Warning: MySQL port $STORAGE_MYSQL_PORT is not listening. Start database first: docker compose --env-file .env up -d" >&2
fi
echo

if [[ "$INSTALL" == true || ! -d "$FRONTEND_DIR/node_modules" ]]; then
  echo "Installing frontend dependencies..."
  (cd "$FRONTEND_DIR" && npm install)
fi

echo "Starting backend on http://localhost:$BACKEND_PORT (MySQL localhost:$STORAGE_MYSQL_PORT) ..."
echo "Default admin: admin / admin123"
echo

cd "$BACKEND_DIR"
mvn spring-boot:run &
BACKEND_PID=$!
if ! wait_backend_ready "$BACKEND_PORT"; then
  echo "Backend did not become ready within 120s. Check the Maven output above (common: MySQL not running on $STORAGE_MYSQL_PORT)." >&2
  kill "$BACKEND_PID" 2>/dev/null || true
  exit 1
fi

echo "Launching frontend on http://localhost:$FRONTEND_PORT/login ..."
(cd "$FRONTEND_DIR" && npm run dev) &
FRONTEND_PID=$!

echo "Launched:"
echo "  Backend  -> http://localhost:$BACKEND_PORT"
echo "  Frontend -> http://localhost:$FRONTEND_PORT/login"
echo
echo "Press Ctrl+C to stop backend and frontend."

wait "$BACKEND_PID"
