#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMEOUT_SECONDS=60
REQUIRE_SEED_DATA=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --timeout)
      TIMEOUT_SECONDS="${2:?--timeout requires seconds}"
      shift 2
      ;;
    --require-seed-data)
      REQUIRE_SEED_DATA=true
      shift
      ;;
    --repo-root)
      ROOT="$(cd "${2:?--repo-root requires a path}" && pwd)"
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

cd "$ROOT"
MYSQL_CONTAINER='storage-mysql'
MYSQL_HOST_PORT=3307
if [[ -f "$ROOT/.env" ]]; then
  import_worktree_env_file "$ROOT"
fi

MYSQL_USER="${MYSQL_USER:-storage}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-storage123}"
MYSQL_DB="${MYSQL_DB:-storage}"

test_mysql_query() {
  local sql="${1:?sql is required}"
  docker exec -e "MYSQL_PWD=$MYSQL_PASSWORD" "$MYSQL_CONTAINER" \
    mysql "-u$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DB" -N -e "$sql" >/tmp/storage-wait-mysql.out 2>/dev/null
}

echo "Waiting for MySQL ($MYSQL_CONTAINER on port $MYSQL_HOST_PORT)..."
deadline=$((SECONDS + TIMEOUT_SECONDS))

while (( SECONDS < deadline )); do
  if test_mysql_query "SELECT 1"; then
    if [[ "$REQUIRE_SEED_DATA" == false ]]; then
      echo "MySQL is ready."
      exit 0
    fi

    if test_mysql_query "SELECT COUNT(*) FROM material_ledger;"; then
      count="$(cat /tmp/storage-wait-mysql.out | tr -d '[:space:]')"
      if [[ "$count" =~ ^[0-9]+$ ]] && (( count > 0 )); then
        echo "MySQL is ready."
        exit 0
      fi
    fi
  fi
  sleep 2
done

echo "MySQL did not become ready within ${TIMEOUT_SECONDS}s (container: $MYSQL_CONTAINER, port: $MYSQL_HOST_PORT)." >&2
exit 1
