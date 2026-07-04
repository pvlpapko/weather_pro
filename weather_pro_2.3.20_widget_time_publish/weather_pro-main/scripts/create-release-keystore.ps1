$ErrorActionPreference = "Stop"
Set-Location "$PSScriptRoot\.."

New-Item -ItemType Directory -Force -Path "app\keystore" | Out-Null
$keystorePath = "app\keystore\weather-pro-release.jks"

if (Test-Path $keystorePath) {
    Write-Host "Keystore already exists: $keystorePath"
    exit 0
}

$storePass = if ($env:WEATHER_KEYSTORE_PASSWORD) { $env:WEATHER_KEYSTORE_PASSWORD } else { "weatherpro123" }
$keyPass = if ($env:WEATHER_KEY_PASSWORD) { $env:WEATHER_KEY_PASSWORD } else { $storePass }
$alias = if ($env:WEATHER_KEY_ALIAS) { $env:WEATHER_KEY_ALIAS } else { "weather-pro" }

keytool -genkeypair `
  -v `
  -keystore $keystorePath `
  -storepass $storePass `
  -keypass $keyPass `
  -alias $alias `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -dname "CN=Weather Pro, OU=LTM_Fedory, O=LTM_Fedory, L=Local, S=Local, C=BY"

Write-Host "Keystore created: $keystorePath"
Write-Host "Use these variables for local release builds:"
Write-Host "`$env:WEATHER_KEYSTORE_PATH = '$((Resolve-Path $keystorePath).Path)'"
Write-Host "`$env:WEATHER_KEYSTORE_PASSWORD = '$storePass'"
Write-Host "`$env:WEATHER_KEY_ALIAS = '$alias'"
Write-Host "`$env:WEATHER_KEY_PASSWORD = '$keyPass'"
