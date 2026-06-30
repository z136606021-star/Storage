#!/usr/bin/env bash

set -euo pipefail

REMOVE_ORPHAN_VOLUMES=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --remove-orphan-volumes)
      REMOVE_ORPHAN_VOLUMES=true
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2
      ;;
  esac
done

legacy_containers=(material-ledger-mysql material-ledger-minio)
legacy_volumes=(storage_mysql_data storage_minio_data)

echo "Cleaning up legacy Docker resources..."
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

if [[ "$REMOVE_ORPHAN_VOLUMES" == true ]]; then
  echo
  echo "Removing orphan legacy volumes..."
  for volume in "${legacy_volumes[@]}"; do
    exists="$(docker volume ls --filter "name=^${volume}$" --format "{{.Name}}" 2>/dev/null || true)"
    if [[ "$exists" == "$volume" ]]; then
      echo "Removing volume: $volume"
      docker volume rm "$volume" >/dev/null
    else
      echo "Volume not found (skip): $volume"
    fi
  done
else
  echo
  echo "Orphan volumes not removed. Pass --remove-orphan-volumes to delete storage_mysql_data / storage_minio_data."
fi

cat <<EOF

Next steps:
  ./scripts/sync-worktree-env.sh
  docker compose --env-file .env up -d
  ./scripts/health-check.sh
EOF
