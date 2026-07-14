# 根据当前 git 分支生成本地 .env（凭据与应用端口；Compose 基础设施已写死在 YAML）
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
$profile = Get-CurrentBranchProfile -RepoRoot $Root
$envPath = Write-WorktreeEnvFile -Profile $profile -RepoRoot $Root

Write-Host "Worktree env synced (dynamic settings only)."
Write-Host "  Branch:   $($profile.Branch)"
Write-Host "  Path:     $Root"
Write-Host "  MySQL:    localhost:3307 (container: storage-mysql)"
Write-Host "  MinIO:    http://localhost:9000 (container: storage-minio)"
Write-Host "  Wrote:    $envPath"
Write-Host ""
Write-Host "Next (dev): docker compose --env-file .env -f docker-compose.yml -f docker-compose-dev.yml up -d"
Write-Host "Production uses a manually maintained .env with external MYSQL_* and MINIO_*; do not run sync-worktree-env for prod."
