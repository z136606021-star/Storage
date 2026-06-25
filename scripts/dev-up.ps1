# One-command dev bootstrap: sync env + docker + wait mysql + start-dev
param(
    [switch]$ResetDb,
    [switch]$SkipDocker,
    [switch]$Install,
    [switch]$NoKill
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
$profile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $profile -RepoRoot $Root | Out-Null
$composeArgs = Get-DockerComposeArgs -RepoRoot $Root

Write-Host "Storage dev-up"
Write-Host "  Branch: $($profile.Branch)"
Write-Host "  MySQL:  localhost:$($profile.MysqlPort)"
Write-Host ""

if ($ResetDb) {
    Write-Host "Resetting database..."
    & (Join-Path $PSScriptRoot 'reset-db.ps1')
    Write-Host ""
} elseif (-not $SkipDocker) {
    Write-Host "Starting Docker (MySQL + MinIO)..."
    docker compose @composeArgs up -d
    & (Join-Path $PSScriptRoot 'wait-mysql.ps1') -RequireSeedData -RepoRoot $Root
    Write-Host ""
}

$startArgs = @()
if ($Install) { $startArgs += '-Install' }
if ($NoKill) { $startArgs += '-NoKill' }

& (Join-Path $PSScriptRoot 'start-dev.ps1') @startArgs
