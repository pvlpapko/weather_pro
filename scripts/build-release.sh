#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -z "${WEATHER_KEYSTORE_PATH:-}" || -z "${WEATHER_KEYSTORE_PASSWORD:-}" || -z "${WEATHER_KEY_ALIAS:-}" || -z "${WEATHER_KEY_PASSWORD:-}" ]]; then
  echo "Release signing variables are not set."
  echo "Set: WEATHER_KEYSTORE_PATH, WEATHER_KEYSTORE_PASSWORD, WEATHER_KEY_ALIAS, WEATHER_KEY_PASSWORD"
  echo "Or run scripts/create-release-keystore.sh first."
  exit 1
fi

echo "Building signed Weather Pro Release APK and AAB..."
gradle assembleRelease bundleRelease --no-daemon

echo "Done:"
echo "- app/build/outputs/apk/release/app-release.apk"
echo "- app/build/outputs/bundle/release/app-release.aab"
