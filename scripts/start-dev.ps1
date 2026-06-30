# 一键启动前后端开发服务（各开一个终端窗口）
# Uses BACKEND_PORT/FRONTEND_PORT from .env and restarts matching Storage dev processes on those ports.
param(
    [switch]$Install,
    [switch]$WithDocker,
    [switch]$NoKill
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $Root "backend"
$FrontendDir = Join-Path $Root "frontend"

. (Join-Path $PSScriptRoot 'worktree-db.ps1')
Set-Location $Root
$DbProfile = Get-CurrentBranchProfile -RepoRoot $Root
Write-WorktreeEnvFile -Profile $DbProfile -RepoRoot $Root | Out-Null
Import-WorktreeEnvFile -RepoRoot $Root
$ComposeArgs = Get-DockerComposeArgs -RepoRoot $Root
$MysqlPort = $DbProfile.MysqlPort

function Get-EnvInt([string]$Name, [int]$DefaultValue) {
    $value = (Get-Item -Path "Env:$Name" -ErrorAction SilentlyContinue).Value
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }

    $parsed = 0
    if ([int]::TryParse($value, [ref]$parsed)) {
        return $parsed
    }

    throw "$Name must be an integer, got '$value'."
}

function ConvertTo-PowerShellSingleQuotedString([string]$Value) {
    if ($null -eq $Value) {
        $Value = ''
    }
    return "'" + ($Value -replace "'", "''") + "'"
}

$BackendPort = Get-EnvInt 'BACKEND_PORT' 8080
$FrontendPort = Get-EnvInt 'FRONTEND_PORT' 5173

function Test-PortInUse([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $conn
}

function Get-PortListenerProcess([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $conn) {
        return $null
    }
    return Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
}

function Get-ProcessCommandLine([int]$ProcessId) {
    try {
        return (Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId").CommandLine
    } catch {
        return $null
    }
}

function Get-ProcessParentId([int]$ProcessId) {
    try {
        return (Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId").ParentProcessId
    } catch {
        return $null
    }
}

function Test-HasDevToolParent([int]$ProcessId, [string[]]$Patterns) {
    $parentId = Get-ProcessParentId $ProcessId
    $depth = 0
    while ($parentId -and $depth -lt 6) {
        $parentCmd = Get-ProcessCommandLine $parentId
        if ($parentCmd) {
            foreach ($pattern in $Patterns) {
                if ($parentCmd -match $pattern) {
                    return $true
                }
            }
        }
        $parentId = Get-ProcessParentId $parentId
        $depth++
    }
    return $false
}

function Test-IsProjectBackendProcess($Process) {
    if ($Process.ProcessName -notin @('java', 'javaw')) {
        return $false
    }
    $cmd = Get-ProcessCommandLine $Process.Id
    if (-not $cmd) {
        return $true
    }
    $escapedRoot = [regex]::Escape($Root)
    $escapedBackendDir = [regex]::Escape($BackendDir)
    if ($cmd -match $escapedRoot -or $cmd -match $escapedBackendDir) {
        return $true
    }
    if ($cmd -match 'spring-boot:run|storage-backend|\\backend\\|com\.storage\.StorageApplication|com\.storage\.') {
        return $true
    }
    # mvn spring-boot:run 实际监听 8080 的是子进程 java，命令行通常只有 -cp @...classpath
    if ($cmd -match 'spring-boot|TieredStopAtLevel=1') {
        return $true
    }
    if (Test-HasDevToolParent $Process.Id @('mvn(\.cmd)?', 'spring-boot:run', 'storage-backend', '\\backend\\')) {
        return $true
    }
    if ($cmd -match 'IntelliJ IDEA|eclipse\.exe|gradle|kafka|zookeeper|elasticsearch') {
        return $false
    }
    # 本地开发：8080 上的 java 默认视为本项目后端，避免误拦重启
    return $true
}

function Test-IsProjectFrontendProcess($Process) {
    if ($Process.ProcessName -ne 'node') {
        return $false
    }
    $cmd = Get-ProcessCommandLine $Process.Id
    if (-not $cmd) {
        return $true
    }
    $escapedRoot = [regex]::Escape($Root)
    $escapedFrontendDir = [regex]::Escape($FrontendDir)
    if ($cmd -match $escapedRoot -or $cmd -match $escapedFrontendDir) {
        return $true
    }
    if ($cmd -match 'vite|npm run dev|\\frontend\\|node_modules[\\/]vite') {
        return $true
    }
    if (Test-HasDevToolParent $Process.Id @('npm(\.cmd)?', 'vite', '\\frontend\\')) {
        return $true
    }
    if ($cmd -match 'IntelliJ IDEA|eclipse\.exe') {
        return $false
    }
    # 本地开发：5173 上的 node 默认视为本项目前端（Vite strictPort）
    return $true
}

function Wait-PortFree([int]$Port, [int]$TimeoutSeconds = 15) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-PortInUse $Port)) {
            return $true
        }
        Start-Sleep -Milliseconds 400
    }
    return -not (Test-PortInUse $Port)
}

function Wait-BackendReady([int]$Port, [int]$TimeoutSeconds = 120) {
    Write-Host "Waiting for backend on http://localhost:$Port ..."
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $healthUrl = "http://localhost:$Port/api/auth/me"

    while ((Get-Date) -lt $deadline) {
        if (Test-PortInUse $Port) {
            try {
                Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop | Out-Null
                Write-Host "Backend is ready." -ForegroundColor Green
                return $true
            } catch {
                $statusCode = $null
                if ($_.Exception.Response) {
                    $statusCode = [int]$_.Exception.Response.StatusCode
                }
                if ($statusCode -eq 401 -or $statusCode -eq 403) {
                    Write-Host "Backend is ready." -ForegroundColor Green
                    return $true
                }
            }
        }
        Start-Sleep -Seconds 2
    }

    return $false
}

function Stop-StaleBackendLaunchers() {
    Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            $_.CommandLine -match 'spring-boot:run' -and
            $_.CommandLine -match [regex]::Escape($BackendDir)
        } |
        ForEach-Object {
            Write-Host "Stopping stale mvn launcher (PID $($_.ProcessId))..." -ForegroundColor Yellow
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

function Resolve-PortConflict {
    param(
        [int]$Port,
        [string]$ServiceName,
        [scriptblock]$IsExpectedProcess
    )

    $proc = Get-PortListenerProcess $Port
    if (-not $proc) {
        return
    }

    $cmdPreview = Get-ProcessCommandLine $proc.Id
    if ($cmdPreview -and $cmdPreview.Length -gt 120) {
        $cmdPreview = $cmdPreview.Substring(0, 120) + '...'
    }

    if (& $IsExpectedProcess $proc) {
        if ($NoKill) {
            Write-Warning "Port $Port is in use by $($proc.ProcessName) (PID $($proc.Id)). Pass without -NoKill to auto-restart $ServiceName."
            return
        }

        Write-Host "Port $Port in use by existing $ServiceName ($($proc.ProcessName), PID $($proc.Id)), stopping..." -ForegroundColor Yellow
        if ($cmdPreview) {
            Write-Host "  $cmdPreview" -ForegroundColor DarkGray
        }
        Stop-Process -Id $proc.Id -Force -ErrorAction Stop
        if (-not (Wait-PortFree $Port)) {
            throw "Port $Port is still in use after stopping PID $($proc.Id)."
        }
        Write-Host "Port $Port is free." -ForegroundColor Green
        return
    }

    $detail = "Port $Port is occupied by $($proc.ProcessName) (PID $($proc.Id))"
    if ($cmdPreview) {
        $detail += ": $cmdPreview"
    }
    throw "$detail. This does not look like the Storage dev $ServiceName. Stop that process manually, or change its port."
}

Write-Host "Storage dev launcher"
Write-Host "Project root: $Root"
Write-Host "Branch:       $($DbProfile.Branch)"
Write-Host "MySQL:        localhost:$MysqlPort ($($DbProfile.MysqlContainer))"
Write-Host ""

if ($WithDocker) {
    Write-Host "Starting Docker (MySQL + MinIO)..."
    Set-Location $Root
    docker compose @ComposeArgs up -d
    & (Join-Path $PSScriptRoot 'wait-mysql.ps1') -RequireSeedData -RepoRoot $Root
    Write-Host "Docker started."
    Write-Host ""
}

Write-Host "Checking ports..."
Resolve-PortConflict -Port $BackendPort -ServiceName "backend" -IsExpectedProcess ${function:Test-IsProjectBackendProcess}
Stop-StaleBackendLaunchers
Resolve-PortConflict -Port $FrontendPort -ServiceName "frontend" -IsExpectedProcess ${function:Test-IsProjectFrontendProcess}

if (-not (Test-PortInUse $MysqlPort)) {
    Write-Warning "MySQL port $MysqlPort is not listening. Start database first: docker compose --env-file .env up -d"
}
Write-Host ""

$backendEnvNames = @(
    'MYSQL_HOST',
    'MYSQL_PORT',
    'MYSQL_DB',
    'MYSQL_USER',
    'MYSQL_PASSWORD',
    'MINIO_ENDPOINT',
    'MINIO_ACCESS_KEY',
    'MINIO_SECRET_KEY',
    'MINIO_BUCKET',
    'BACKEND_PORT',
    'CORS_ALLOWED_ORIGINS',
    'SESSION_COOKIE_HTTP_ONLY',
    'SESSION_COOKIE_SECURE',
    'RESET_ADMIN_PASSWORD_ON_STARTUP',
    'UPLOAD_MAX_SIZE_BYTES',
    'UPLOAD_ALLOWED_CONTENT_TYPES',
    'APP_PUBLIC_BASE_URL',
    'PASSWORD_RESET_TOKEN_TTL_MINUTES',
    'MAIL_HOST',
    'MAIL_PORT',
    'MAIL_USERNAME',
    'MAIL_PASSWORD',
    'MAIL_FROM',
    'MAIL_SMTP_AUTH',
    'MAIL_SMTP_STARTTLS_ENABLE'
)
$backendEnvAssignments = ($backendEnvNames | ForEach-Object {
    $value = (Get-Item -Path "Env:$_" -ErrorAction SilentlyContinue).Value
    "`$env:$_ = $(ConvertTo-PowerShellSingleQuotedString $value)"
}) -join "`r`n"
$backendDirLiteral = ConvertTo-PowerShellSingleQuotedString $BackendDir
$frontendDirLiteral = ConvertTo-PowerShellSingleQuotedString $FrontendDir

$backendCommand = @"
Set-Location $backendDirLiteral
`$ErrorActionPreference = 'Continue'
$backendEnvAssignments
Write-Host 'Starting backend on http://localhost:$BackendPort (MySQL localhost:$MysqlPort) ...' -ForegroundColor Cyan
mvn spring-boot:run
if (`$LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host 'Backend exited with error. See messages above.' -ForegroundColor Red
}
"@

$frontendSetup = if ($Install -or -not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    "npm install; "
} else {
    ""
}

$frontendCommand = @"
Set-Location $frontendDirLiteral
Write-Host 'Starting frontend on http://localhost:$FrontendPort/login ...' -ForegroundColor Cyan
$frontendSetup npm run dev
"@

Write-Host "Launching backend..."
Start-Process powershell -ArgumentList "-NoExit", "-NoProfile", "-Command", $backendCommand

if (-not (Wait-BackendReady -Port $BackendPort)) {
    throw "Backend did not become ready within 120s. Open the backend PowerShell window for errors (common: MySQL not running on $MysqlPort)."
}

Write-Host "Launching frontend..."
Start-Process powershell -ArgumentList "-NoExit", "-NoProfile", "-Command", $frontendCommand

Write-Host "Launched:"
Write-Host "  Backend  -> http://localhost:$BackendPort"
Write-Host "  Frontend -> http://localhost:$FrontendPort/login"
Write-Host ""
Write-Host "Default admin: admin / admin123"
Write-Host "Close the two PowerShell windows to stop the services."
Write-Host "Re-run this script anytime to auto-restart dev servers on ports $BackendPort / $FrontendPort."
