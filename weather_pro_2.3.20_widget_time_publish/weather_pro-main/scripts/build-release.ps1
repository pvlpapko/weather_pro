$ErrorActionPreference = "Stop"
Set-Location "$PSScriptRoot\.."

if (-not [Environment]::GetEnvironmentVariable("WEATHER_KEYSTORE_PATH") -and -not [Environment]::GetEnvironmentVariable("LTM_KEYSTORE_PATH")) {
    Write-Host "Release signing variables are not set."
    Write-Host "Using stable repository key: app/dev-update-key.jks"
    Write-Host "For store publication, set WEATHER_* GitHub Secrets or local WEATHER_* variables with your permanent release/upload key."
}

Write-Host "Building signed Weather Pro Release APK and AAB..."
gradle assembleRelease bundleRelease --no-daemon
Write-Host "Done:"
Write-Host "- app/build/outputs/apk/release/app-release.apk"
Write-Host "- app/build/outputs/bundle/release/app-release.aab"
