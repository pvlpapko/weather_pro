#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Cleaning Weather Widget Pro build files..."
gradle clean --no-daemon
