$ErrorActionPreference = "Stop"
Set-Location "$PSScriptRoot\.."
Write-Host "Building Weather Widget Pro Debug APK..."
gradle assembleDebug --no-daemon
Write-Host "Done: app/build/outputs/apk/debug/app-debug.apk"
