$ErrorActionPreference = "Stop"
Set-Location "$PSScriptRoot\.."

$keystorePath = if ($args.Count -gt 0) { $args[0] } else { "app\keystore\weather-pro-release.jks" }

if (-not (Test-Path $keystorePath)) {
    throw "Keystore not found: $keystorePath. Run scripts/create-release-keystore.ps1 first or pass a keystore path."
}

[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $keystorePath).Path))
