#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "dev-up.sh is deprecated. Use scripts/deploy-cli.sh --profile dev [--build]." >&2
"$SCRIPT_DIR/deploy-cli.sh" --profile dev
