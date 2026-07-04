#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p app/keystore
KEYSTORE_PATH="app/keystore/weather-pro-release.jks"

if [[ -f "$KEYSTORE_PATH" ]]; then
  echo "Keystore already exists: $KEYSTORE_PATH"
  exit 0
fi

STORE_PASS="${WEATHER_KEYSTORE_PASSWORD:-weatherpro123}"
KEY_PASS="${WEATHER_KEY_PASSWORD:-$STORE_PASS}"
ALIAS="${WEATHER_KEY_ALIAS:-weather-pro}"

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Weather Pro, OU=LTM_Fedory, O=LTM_Fedory, L=Local, S=Local, C=BY"

echo "Keystore created: $KEYSTORE_PATH"
echo "Use these variables for local release builds:"
echo "export WEATHER_KEYSTORE_PATH=$PWD/$KEYSTORE_PATH"
echo "export WEATHER_KEYSTORE_PASSWORD=$STORE_PASS"
echo "export WEATHER_KEY_ALIAS=$ALIAS"
echo "export WEATHER_KEY_PASSWORD=$KEY_PASS"
