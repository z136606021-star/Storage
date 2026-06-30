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
if (Test-Path -LiteralPath (Join-Path $RepoRoot '.env')) {
    Import-WorktreeEnvFile -RepoRoot $RepoRoot
}
$mysqlUser = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { 'storage' }
$mysqlPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { 'storage123' }
$mysqlDb = if ($env:MYSQL_DB) { $env:MYSQL_DB } else { 'storage' }

function Test-PortOpen([int]$TargetPort) {
    $conn = Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $conn
}

function Test-MysqlQuery([string]$Sql) {
    $mysqlArgs = @(
        'exec', '-e', "MYSQL_PWD=$mysqlPassword", $container,
        'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
        $mysqlDb, '-N', '-e', $Sql
    )
    $output = docker @mysqlArgs 2>$null
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
            $mysqlArgs = @(
                'exec', '-e', "MYSQL_PWD=$mysqlPassword", $container,
                'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
                $mysqlDb, '-N', '-e', 'SELECT COUNT(*) FROM material_ledger;'
            )
            $count = docker @mysqlArgs 2>$null
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
