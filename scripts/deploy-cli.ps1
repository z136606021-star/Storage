param(
    [ValidateSet('dev', 'prod')]
    [string]$Profile = 'dev',
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $Root
Write-WorktreeEnvFile -Profile (Get-CurrentBranchProfile -RepoRoot $Root) -RepoRoot $Root | Out-Null
$composeArgs = Get-DockerComposeArgs -RepoRoot $Root

$composeFiles = @('-f', 'docker-compose.yml')
if ($Profile -eq 'dev') {
    $composeFiles += @('-f', 'docker-compose-dev.yml')
}

$upArgs = @('up', '-d')
if ($Build) {
    $upArgs += '--build'
}

Write-Host "Deploy profile: $Profile"
docker compose @composeArgs @composeFiles @upArgs
Write-Host "Done. Services are running in background."
