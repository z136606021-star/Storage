# 根据当前 git 分支生成本地 .env（MySQL/MinIO 端口与 Docker 卷隔离）
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
$profile = Get-CurrentBranchProfile -RepoRoot $Root
$envPath = Write-WorktreeEnvFile -Profile $profile -RepoRoot $Root

Write-Host "Worktree DB profile synced."
Write-Host "  Branch:   $($profile.Branch)"
Write-Host "  Path:     $Root"
Write-Host "  MySQL:    localhost:$($profile.MysqlPort) (container: $($profile.MysqlContainer))"
Write-Host "  MinIO:    http://localhost:$($profile.MinioPort) (console: $($profile.MinioConsolePort))"
Write-Host "  Compose:  $($profile.ComposeProjectName)"
Write-Host "  Wrote:    $envPath"
Write-Host ""
Write-Host "Next: docker compose --env-file .env up -d"
