# Remove pre-worktree-isolation Docker containers (material-ledger-*)
$ErrorActionPreference = "Stop"

$legacyContainers = @('material-ledger-mysql', 'material-ledger-minio')

Write-Host "Cleaning up legacy Docker containers..."
Write-Host ""
Write-Host "Note: this script only removes old material-ledger-* containers."
Write-Host "It does not delete Docker volumes or database/object storage data."
Write-Host "Use DBeaver or MinIO Console for manual data maintenance."
Write-Host ""

foreach ($name in $legacyContainers) {
    $exists = docker ps -a --filter "name=^/${name}$" --format "{{.Names}}" 2>$null
    if ($exists -eq $name) {
        Write-Host "Removing container: $name"
        docker rm -f $name 2>$null | Out-Null
    } else {
        Write-Host "Container not found (skip): $name"
    }
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "  .\scripts\sync-worktree-env.ps1"
Write-Host "  docker compose --env-file .env up -d"
Write-Host "  .\scripts\health-check.ps1"
