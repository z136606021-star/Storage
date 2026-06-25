# Wait until MySQL is ready (port + docker exec SELECT 1)
param(
    [int]$TimeoutSeconds = 60,
    [switch]$RequireSeedData,
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $RepoRoot
$profile = Get-CurrentBranchProfile -RepoRoot $RepoRoot
$container = $profile.MysqlContainer
$port = $profile.MysqlPort

function Test-PortOpen([int]$TargetPort) {
    $conn = Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $conn
}

function Test-MysqlQuery([string]$Sql) {
    $output = docker exec $container mysql -ustorage -pstorage123 --default-character-set=utf8mb4 storage -N -e $Sql 2>$null
    return $LASTEXITCODE -eq 0 -and $null -ne $output
}

Write-Host "Waiting for MySQL ($container on port $port)..."
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

while ((Get-Date) -lt $deadline) {
    $portReady = Test-PortOpen $port
    $pingReady = $false
    $seedReady = -not $RequireSeedData

    if ($portReady) {
        $pingReady = Test-MysqlQuery 'SELECT 1'
        if ($pingReady -and $RequireSeedData) {
            $count = docker exec $container mysql -ustorage -pstorage123 --default-character-set=utf8mb4 storage -N -e 'SELECT COUNT(*) FROM material_ledger;' 2>$null
            if ($LASTEXITCODE -eq 0 -and [int]$count -gt 0) {
                $seedReady = $true
            }
        }
    }

    if ($portReady -and $pingReady -and $seedReady) {
        Write-Host "MySQL is ready." -ForegroundColor Green
        return
    }

    Start-Sleep -Seconds 2
}

throw "MySQL did not become ready within ${TimeoutSeconds}s (container: $container, port: $port)."
