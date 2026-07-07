#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "start-dev.sh is deprecated. Use scripts/deploy-cli.sh --profile dev." >&2
"$SCRIPT_DIR/deploy-cli.sh" --profile dev
