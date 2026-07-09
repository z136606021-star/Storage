param(
    [switch]$NoOpenBrowser,
    [switch]$SkipNpmInstall
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

function Stop-ListenerOnPort {
    param(
        [int]$Port,
        [string]$Label
    )

    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    } catch {
        return
    }

    if (-not $connections) {
        return
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if (-not $process) {
            continue
        }
        if ($process.ProcessName -match '^(com\.docker|Docker|wslrelay|wslhost)$') {
            continue
        }
        Write-Host "Stopping stale $Label process $($process.ProcessName) (PID $processId) on port $Port ..."
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

function New-DevLauncherScript {
    param(
        [string]$Path,
        [string]$Title,
        [string[]]$Lines
    )

    $content = @(
        '@echo off'
        "title $Title"
        'setlocal EnableExtensions'
    ) + $Lines + @(
        'endlocal'
    )
    [System.IO.File]::WriteAllText($Path, ($content -join [Environment]::NewLine), [System.Text.UTF8Encoding]::new($false))
}

function Wait-MySqlHealthy {
    param(
        [string[]]$ComposeArgs,
        [string[]]$ComposeFiles,
        [int]$TimeoutSeconds = 120
    )

    Write-Host 'Waiting for MySQL to become healthy ...'
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $ping = Invoke-ExternalCommand -Command {
            docker compose @ComposeArgs @ComposeFiles exec -T mysql sh -c 'mysqladmin ping -h localhost -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent'
        }
        if ($ping.ExitCode -eq 0) {
            Write-Host 'MySQL is ready.'
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw 'MySQL did not become healthy in time. Check: docker compose logs mysql'
}

function Wait-HttpEndpoint {
    param(
        [string]$Url,
        [string]$Name,
        [int]$TimeoutSeconds = 120
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

Write-Host 'Storage local dev: Vite HMR + spring-boot:run (code changes auto-refresh)'
Write-Host ''

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is required for MySQL/MinIO. Install Docker Desktop and retry.'
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven is required. Install Java 17+ and Maven, then retry.'
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw 'Node.js/npm is required. Install Node.js 20+, then retry.'
}

$branchProfile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $branchProfile -RepoRoot $Root | Out-Null
$envValues = Read-DotEnvValues -EnvPath (Join-Path $Root '.env')

$backendPort = [int](Get-EnvOrExistingValue -Existing $envValues -Name 'BACKEND_PORT' -DefaultValue '8080')
$frontendPort = [int](Get-EnvOrExistingValue -Existing $envValues -Name 'FRONTEND_PORT' -DefaultValue '5173')
$mysqlHostPort = Get-EnvOrExistingValue -Existing $envValues -Name 'STORAGE_MYSQL_PORT' -DefaultValue '3307'
$minioHostPort = Get-EnvOrExistingValue -Existing $envValues -Name 'STORAGE_MINIO_PORT' -DefaultValue '9000'

$composeArgs = Get-DockerComposeArgs -RepoRoot $Root
$composeFiles = @('-f', 'docker-compose.yml', '-f', 'docker-compose-dev.yml')

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    $previousErrorAction = $ErrorActionPreference
    $previousNativeErrorPref = $null
    if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
        $previousNativeErrorPref = $PSNativeCommandUseErrorActionPreference
        $PSNativeCommandUseErrorActionPreference = $false
    }

    $ErrorActionPreference = 'Continue'
    try {
        $output = & $Command 2>&1
        $exitCode = if ($null -ne $LASTEXITCODE) { $LASTEXITCODE } else { 0 }
        return [pscustomobject]@{
            Output = $output
            ExitCode = $exitCode
        }
    } finally {
        $ErrorActionPreference = $previousErrorAction
        if ($null -ne $previousNativeErrorPref) {
            $PSNativeCommandUseErrorActionPreference = $previousNativeErrorPref
        }
    }
}

function Invoke-DockerCompose {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$ComposeArgs,
        [Parameter(Mandatory = $true)]
        [string[]]$ComposeFiles,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $result = Invoke-ExternalCommand -Command {
        docker compose @ComposeArgs @ComposeFiles @Arguments
    }
    if ($result.Output) {
        $result.Output | ForEach-Object { Write-Host $_ }
    }
    return $result.ExitCode
}

Write-Host 'Stopping Docker backend/frontend containers (if any) ...'
Invoke-DockerCompose -ComposeArgs $composeArgs -ComposeFiles $composeFiles -Arguments @('stop', 'backend', 'frontend') | Out-Null

Write-Host 'Starting Docker dependencies: mysql, minio ...'
$upExitCode = Invoke-DockerCompose -ComposeArgs $composeArgs -ComposeFiles $composeFiles -Arguments @('up', '-d', 'mysql', 'minio')
if ($upExitCode -ne 0) {
    throw 'Failed to start mysql/minio. Ensure Docker Desktop is running.'
}
Wait-MySqlHealthy -ComposeArgs $composeArgs -ComposeFiles $composeFiles

Stop-ListenerOnPort -Port $backendPort -Label 'backend'
Stop-ListenerOnPort -Port $frontendPort -Label 'frontend'

if (-not $SkipNpmInstall) {
    $nodeModules = Join-Path $Root 'frontend\node_modules'
    if (-not (Test-Path -LiteralPath $nodeModules)) {
        Write-Host 'Installing frontend dependencies (first run) ...'
        npm install --prefix (Join-Path $Root 'frontend')
        if ($LASTEXITCODE -ne 0) {
            throw 'npm install failed in frontend/'
        }
    }
}

$launcherDir = Join-Path $env:TEMP 'storage-dev-launchers'
New-Item -ItemType Directory -Force -Path $launcherDir | Out-Null

$backendEnvLines = @(
    "set MYSQL_HOST=127.0.0.1"
    "set MYSQL_PORT=$mysqlHostPort"
    "set MINIO_ENDPOINT=http://127.0.0.1:$minioHostPort"
    "set BACKEND_PORT=$backendPort"
    "set APP_PUBLIC_BASE_URL=http://localhost:$frontendPort"
)

foreach ($name in @(
        'MYSQL_DB', 'MYSQL_USER', 'MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD',
        'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY', 'MINIO_BUCKET',
        'RESET_ADMIN_PASSWORD_ON_STARTUP', 'JWT_SECRET', 'JWT_TTL_MINUTES',
        'SESSION_COOKIE_HTTP_ONLY', 'SESSION_COOKIE_SECURE',
        'UPLOAD_MAX_SIZE_BYTES', 'UPLOAD_ALLOWED_CONTENT_TYPES',
        'PASSWORD_RESET_TOKEN_TTL_MINUTES',
        'MAIL_HOST', 'MAIL_PORT', 'MAIL_USERNAME', 'MAIL_PASSWORD', 'MAIL_FROM',
        'MAIL_SMTP_AUTH', 'MAIL_SMTP_STARTTLS_ENABLE'
    )) {
    if ($envValues.ContainsKey($name)) {
        $backendEnvLines += "set $name=$($envValues[$name])"
    }
}

$backendLauncher = Join-Path $launcherDir 'storage-backend-dev.cmd'
New-DevLauncherScript -Path $backendLauncher -Title 'Storage Backend (spring-boot:run)' -Lines @(
    $backendEnvLines
    "cd /d `"$Root\backend`""
    "echo Backend: http://localhost:$backendPort"
    'mvn spring-boot:run'
    'echo.'
    'echo Backend stopped.'
    'pause'
)

$frontendLauncher = Join-Path $launcherDir 'storage-frontend-dev.cmd'
New-DevLauncherScript -Path $frontendLauncher -Title 'Storage Frontend (Vite HMR)' -Lines @(
    "set FRONTEND_PORT=$frontendPort"
    "set VITE_API_PROXY=http://localhost:$backendPort"
    "cd /d `"$Root\frontend`""
    "echo Frontend: http://localhost:$frontendPort/login"
    'npm run dev'
    'echo.'
    'echo Frontend stopped.'
    'pause'
)

Write-Host 'Launching local backend and frontend dev servers ...'
Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $backendLauncher | Out-Null
Start-Sleep -Seconds 2
Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $frontendLauncher | Out-Null

Wait-HttpEndpoint -Url "http://localhost:$backendPort/health" -Name 'backend' -TimeoutSeconds 180
Wait-HttpEndpoint -Url "http://localhost:$frontendPort/login" -Name 'frontend' -TimeoutSeconds 120

$appUrl = "http://localhost:$frontendPort/login"
Write-Host ''
Write-Host 'Local dev is running:'
Write-Host "  Frontend (Vite HMR): $appUrl"
Write-Host "  Backend API:         http://localhost:$backendPort"
Write-Host "  MySQL:               localhost:$mysqlHostPort"
Write-Host "  MinIO:               http://localhost:$minioHostPort"
Write-Host ''
Write-Host 'Tips:'
Write-Host '  - Frontend edits refresh automatically in the browser.'
Write-Host '  - Backend Java edits trigger spring-boot:run restart; keep the backend window open.'
Write-Host '  - Re-run start-dev.cmd to restart dev servers and free stale ports.'
Write-Host '  - For full Docker deploy (no HMR), use dev-up.cmd instead.'

if (-not $NoOpenBrowser) {
    Write-Host "Opening $appUrl ..."
    Start-Process $appUrl
}
