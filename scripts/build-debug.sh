#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building Weather Widget Pro Debug APK..."
gradle assembleDebug --no-daemon

echo "Done: app/build/outputs/apk/debug/app-debug.apk"
