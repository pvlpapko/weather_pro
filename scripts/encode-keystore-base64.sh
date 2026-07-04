#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE_PATH="${1:-app/keystore/weather-pro-release.jks}"

if [[ ! -f "$KEYSTORE_PATH" ]]; then
  echo "Keystore not found: $KEYSTORE_PATH"
  echo "Run scripts/create-release-keystore.sh first or pass a keystore path."
  exit 1
fi

if base64 --help 2>&1 | grep -q -- '-w'; then
  base64 -w 0 "$KEYSTORE_PATH"
else
  base64 "$KEYSTORE_PATH" | tr -d '\n'
fi

echo
