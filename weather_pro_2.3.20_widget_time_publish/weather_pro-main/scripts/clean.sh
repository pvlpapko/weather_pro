#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Cleaning Weather Pro build files..."
gradle clean --no-daemon
