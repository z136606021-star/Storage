#!/usr/bin/env bash

set -euo pipefail

legacy_containers=(material-ledger-mysql material-ledger-minio)

echo "Cleaning up legacy Docker containers..."
echo
echo "Note: this script only removes old material-ledger-* containers."
echo "It does not delete Docker volumes or database/object storage data."
echo "Use DBeaver or MinIO Console for manual data maintenance."
echo

for name in "${legacy_containers[@]}"; do
  exists="$(docker ps -a --filter "name=^/${name}$" --format "{{.Names}}" 2>/dev/null || true)"
  if [[ "$exists" == "$name" ]]; then
    echo "Removing container: $name"
    docker rm -f "$name" >/dev/null
  else
    echo "Container not found (skip): $name"
  fi
done

cat <<EOF

Next steps:
  ./scripts/sync-worktree-env.sh
  docker compose --env-file .env up -d
  ./scripts/health-check.sh
EOF
