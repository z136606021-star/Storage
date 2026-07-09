# Deprecated wrapper kept for compatibility.
Write-Warning "start-dev.ps1 now launches local hot-reload dev. For Docker-only deploy use scripts/deploy-cli.ps1 -Profile dev."
& (Join-Path $PSScriptRoot 'start-dev-local.ps1') @args
