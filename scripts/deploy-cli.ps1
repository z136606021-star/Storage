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

function Assert-DevPortsAvailable {
    param(
        [string]$RepoRoot,
        [string]$ComposeProjectName
    )

    $envValues = Read-DotEnvValues -EnvPath (Join-Path $RepoRoot '.env')
    $ports = @(
        Get-EnvOrExistingValue -Existing $envValues -Name 'FRONTEND_PORT' -DefaultValue '5173'
        Get-EnvOrExistingValue -Existing $envValues -Name 'BACKEND_PORT' -DefaultValue '8080'
        Get-EnvOrExistingValue -Existing $envValues -Name 'STORAGE_MYSQL_PORT' -DefaultValue '3307'
        Get-EnvOrExistingValue -Existing $envValues -Name 'STORAGE_MINIO_PORT' -DefaultValue '9000'
    ) | Sort-Object -Unique

    $rows = docker ps --format "{{.Names}}|{{.Ports}}" 2>$null
    if ($LASTEXITCODE -ne 0) {
        return
    }

    $conflicts = @()
    foreach ($row in $rows) {
        $parts = $row -split '\|', 2
        if ($parts.Count -lt 2) {
            continue
        }
        $name = $parts[0]
        $portText = $parts[1]
        if ($name -like "$ComposeProjectName-*") {
            continue
        }
        foreach ($port in $ports) {
            if ($portText -match "(^|[:,\[]|0\.0\.0\.0:|127\.0\.0\.1:|\[::\]:)$([regex]::Escape($port))->") {
                $conflicts += "  port $port is already used by container $name ($portText)"
            }
        }
    }

    if ($conflicts.Count -gt 0) {
        $message = @(
            "Cannot start dev profile because required ports are already in use by another Docker project."
            "Stop the conflicting project first, for example: docker compose -p <project-name> down"
            "Conflicts:"
            ($conflicts | Sort-Object -Unique)
        ) -join [Environment]::NewLine
        throw $message
    }
}

Set-Location $Root
$currentProfile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $currentProfile -RepoRoot $Root | Out-Null
$composeArgs = Get-DockerComposeArgs -RepoRoot $Root

$composeFiles = @('-f', 'docker-compose.yml')
if ($Profile -eq 'dev') {
    $composeFiles += @('-f', 'docker-compose-dev.yml')
    Assert-DevPortsAvailable -RepoRoot $Root -ComposeProjectName $currentProfile.ComposeProjectName
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
