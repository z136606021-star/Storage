# 重置本 worktree 的 MySQL + MinIO（独立卷，按当前分支端口）
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
$profile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $profile -RepoRoot $Root | Out-Null
Import-WorktreeEnvFile -RepoRoot $Root
$composeArgs = Get-DockerComposeArgs -RepoRoot $Root
$mysqlUser = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { 'storage' }
$mysqlPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { 'storage123' }
$mysqlDb = if ($env:MYSQL_DB) { $env:MYSQL_DB } else { 'storage' }

Write-Host "Resetting database for branch: $($profile.Branch)"
Write-Host "  MySQL port: $($profile.MysqlPort)"
Write-Host "  Container:  $($profile.MysqlContainer)"
Write-Host "  Volume:     $($profile.MysqlVolume)"
Write-Host ""

Write-Host "Stopping containers and removing volumes..."
docker compose @composeArgs down -v

Write-Host "Starting fresh MySQL + MinIO..."
docker compose @composeArgs up -d

Write-Host "Waiting for MySQL to initialize..."
& (Join-Path $PSScriptRoot 'wait-mysql.ps1') -RequireSeedData -RepoRoot $Root

$mysqlExec = @(
    'exec', '-e', "MYSQL_PWD=$mysqlPassword", $profile.MysqlContainer,
    'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
    $mysqlDb, '-N', '-e', 'SELECT COUNT(*) FROM material_ledger;'
)
$count = docker @mysqlExec 2>$null
Write-Host "material_ledger rows: $count"

$mysqlSample = @(
    'exec', '-e', "MYSQL_PWD=$mysqlPassword", $profile.MysqlContainer,
    'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
    $mysqlDb, '-e', 'SELECT id, category, name FROM material_ledger LIMIT 3;'
)
docker @mysqlSample 2>$null

Write-Host "Done. Restart backend: .\scripts\start-dev.ps1"
