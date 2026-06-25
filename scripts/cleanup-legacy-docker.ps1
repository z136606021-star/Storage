# Remove pre-worktree-isolation Docker containers (material-ledger-*)
param(
    [switch]$RemoveOrphanVolumes
)

$ErrorActionPreference = "Stop"

$legacyContainers = @('material-ledger-mysql', 'material-ledger-minio')
$legacyVolumes = @('storage_mysql_data', 'storage_minio_data')

Write-Host "Cleaning up legacy Docker resources..."
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

if ($RemoveOrphanVolumes) {
    Write-Host ""
    Write-Host "Removing orphan legacy volumes..."
    foreach ($vol in $legacyVolumes) {
        $exists = docker volume ls --filter "name=^${vol}$" --format "{{.Name}}" 2>$null
        if ($exists -eq $vol) {
            Write-Host "Removing volume: $vol"
            docker volume rm $vol 2>$null | Out-Null
        } else {
            Write-Host "Volume not found (skip): $vol"
        }
    }
} else {
    Write-Host ""
    Write-Host "Orphan volumes not removed. Pass -RemoveOrphanVolumes to delete storage_mysql_data / storage_minio_data."
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "  .\scripts\sync-worktree-env.ps1"
Write-Host "  docker compose --env-file .env up -d"
Write-Host "  .\scripts\health-check.ps1"
