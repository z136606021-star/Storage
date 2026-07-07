param(
    [ValidateSet('dev', 'prod')]
    [string]$Profile = 'dev',
    [switch]$Build,
    [switch]$NoOpenBrowser
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

function Open-DeployUrl {
    param(
        [string]$ProfileName,
        [string]$RepoRoot
    )

    $envValues = Read-DotEnvValues -EnvPath (Join-Path $RepoRoot '.env')
    $port = if ($ProfileName -eq 'dev') {
        Get-EnvOrExistingValue -Existing $envValues -Name 'FRONTEND_PORT' -DefaultValue '5173'
    } else {
        Get-EnvOrExistingValue -Existing $envValues -Name 'APP_PORT' -DefaultValue '80'
    }
    $url = "http://localhost:$port"
    Write-Host "Opening $url ..."
    Start-Process $url
}

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
if (-not $NoOpenBrowser) {
    Open-DeployUrl -ProfileName $Profile -RepoRoot $Root
}
Write-Host "Done. Services are running in background."
