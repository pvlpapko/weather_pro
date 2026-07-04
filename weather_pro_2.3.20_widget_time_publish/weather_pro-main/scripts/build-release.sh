#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -z "${WEATHER_KEYSTORE_PATH:-}" && -z "${LTM_KEYSTORE_PATH:-}" ]]; then
  echo "Release signing variables are not set."
  echo "Using stable repository key: app/dev-update-key.jks"
  echo "For store publication, set WEATHER_* GitHub Secrets or local WEATHER_* variables with your permanent release/upload key."
fi

echo "Building signed Weather Pro Release APK and AAB..."
gradle assembleRelease bundleRelease --no-daemon

echo "Done:"
echo "- app/build/outputs/apk/release/app-release.apk"
echo "- app/build/outputs/bundle/release/app-release.aab"
