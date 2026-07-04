$ErrorActionPreference = "Stop"
Set-Location "$PSScriptRoot\.."

$required = @("WEATHER_KEYSTORE_PATH", "WEATHER_KEYSTORE_PASSWORD", "WEATHER_KEY_ALIAS", "WEATHER_KEY_PASSWORD")
foreach ($name in $required) {
    if (-not [Environment]::GetEnvironmentVariable($name)) {
        throw "Release signing variable is not set: $name. Run scripts/create-release-keystore.ps1 first or set all signing variables."
    }
}

Write-Host "Building signed Weather Pro Release APK and AAB..."
gradle assembleRelease bundleRelease --no-daemon
Write-Host "Done:"
Write-Host "- app/build/outputs/apk/release/app-release.apk"
Write-Host "- app/build/outputs/bundle/release/app-release.aab"
