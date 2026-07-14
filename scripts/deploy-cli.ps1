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

    $url = Get-DeployUrl -ProfileName $ProfileName -RepoRoot $RepoRoot
    Write-Host "Opening $url ..."
    Start-Process $url
}

function Get-DeployUrl {
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
    return "http://localhost:$port"
}

function Wait-HttpEndpoint {
    param(
        [string]$Url,
        [string]$Name,
        [int]$TimeoutSeconds = 90
    )

    Write-Host "Waiting for $Name at $Url ..."
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3 -ErrorAction Stop | Out-Null
            Write-Host "$Name is ready."
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw "$Name did not become ready within $TimeoutSeconds seconds: $Url"
}

function Wait-DeployReady {
    param(
        [string]$ProfileName,
        [string]$RepoRoot
    )

    $envValues = Read-DotEnvValues -EnvPath (Join-Path $RepoRoot '.env')
    if ($ProfileName -eq 'dev') {
        $backendPort = Get-EnvOrExistingValue -Existing $envValues -Name 'BACKEND_PORT' -DefaultValue '8080'
        Wait-HttpEndpoint -Url "http://localhost:$backendPort/health" -Name 'backend'
    }

    Wait-HttpEndpoint -Url (Get-DeployUrl -ProfileName $ProfileName -RepoRoot $RepoRoot) -Name 'frontend'
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

function Assert-ProdEnvReady {
    param(
        [string]$RepoRoot
    )

    $envPath = Join-Path $RepoRoot '.env'
    if (-not (Test-Path -LiteralPath $envPath)) {
        throw @(
            "Production deploy requires an existing .env at $envPath."
            "Create it from .env.example and set external MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET."
            "Production does not auto-generate .env; use sync-worktree-env only for local dev."
        ) -join [Environment]::NewLine
    }

    $envValues = Read-DotEnvValues -EnvPath $envPath
    $required = @('MINIO_ENDPOINT', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY', 'MINIO_BUCKET')
    $missing = @()
    foreach ($name in $required) {
        if (-not $envValues.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($envValues[$name])) {
            $missing += $name
        }
    }
    if ($missing.Count -gt 0) {
        throw "Production .env is missing required MinIO settings: $($missing -join ', '). Configure external MinIO in .env before deploying prod."
    }
}

Set-Location $Root
$currentProfile = Get-CurrentBranchProfile -RepoRoot $Root
if ($Profile -eq 'dev') {
    Write-WorktreeEnvFile -Profile $currentProfile -RepoRoot $Root | Out-Null
} else {
    Assert-ProdEnvReady -RepoRoot $Root
}
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
Wait-DeployReady -ProfileName $Profile -RepoRoot $Root
if (-not $NoOpenBrowser) {
    Open-DeployUrl -ProfileName $Profile -RepoRoot $Root
}
Write-Host "Done. Services are running in background."
