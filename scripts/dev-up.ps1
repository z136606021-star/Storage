# Deprecated wrapper kept for compatibility.
Write-Warning "dev-up.ps1 is deprecated. Use scripts/deploy-cli.ps1 -Profile dev."
& (Join-Path $PSScriptRoot 'deploy-cli.ps1') -Profile dev @args
