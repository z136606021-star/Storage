# 重置本 worktree 的 MySQL + MinIO（独立卷，按当前分支端口）
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
$profile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $profile -RepoRoot $Root | Out-Null
$composeArgs = Get-DockerComposeArgs -RepoRoot $Root

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
    'docker', 'exec', $profile.MysqlContainer,
    'mysql', '-ustorage', '-pstorage123', '--default-character-set=utf8mb4',
    'storage', '-N', '-e', 'SELECT COUNT(*) FROM material_ledger;'
)
$count = & $mysqlExec[0] $mysqlExec[1..($mysqlExec.Length - 1)] 2>$null
Write-Host "material_ledger rows: $count"

$mysqlSample = @(
    'docker', 'exec', $profile.MysqlContainer,
    'mysql', '-ustorage', '-pstorage123', '--default-character-set=utf8mb4',
    'storage', '-e', 'SELECT id, category, name FROM material_ledger LIMIT 3;'
)
& $mysqlSample[0] $mysqlSample[1..($mysqlSample.Length - 1)] 2>$null

Write-Host "Done. Restart backend: .\scripts\start-dev.ps1"
