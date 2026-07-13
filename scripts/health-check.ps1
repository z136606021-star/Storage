# Read-only dev environment health check (exit 0 = all pass, 1 = any fail)
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Continue"

. (Join-Path $PSScriptRoot 'worktree-db.ps1')

Set-Location $RepoRoot
$failures = 0

function Write-CheckResult([string]$Name, [bool]$Ok, [string]$Detail) {
    if ($Ok) {
        Write-Host "[OK]   $Name - $Detail" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $Name - $Detail" -ForegroundColor Red
        $script:failures++
    }
}

function Test-PortInUse([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $conn
}

function Test-ContainerRunning([string]$Name) {
    $status = docker inspect -f '{{.State.Running}}' $Name 2>$null
    return $status -eq 'true'
}

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

function Get-EnvText([string]$Name, [string]$DefaultValue) {
    $value = (Get-Item -Path "Env:$Name" -ErrorAction SilentlyContinue).Value
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value
}

Write-Host "Storage dev health check"
Write-Host "Project root: $RepoRoot"
Write-Host ""

try {
    $profile = Get-CurrentBranchProfile -RepoRoot $RepoRoot
    $normalizedRoot = (Resolve-Path -LiteralPath $RepoRoot).Path -replace '\\', '/'
    $normalizedExpected = $profile.WorktreePath -replace '\\', '/'
    $pathOk = $normalizedRoot -eq $normalizedExpected
    Write-CheckResult 'branch' $true "branch=$($profile.Branch)"
    Write-CheckResult 'worktree-path' $pathOk "expected=$normalizedExpected actual=$normalizedRoot"
} catch {
    Write-CheckResult 'branch' $false $_.Exception.Message
    $profile = $null
}

$envPath = Join-Path $RepoRoot '.env'
$envOk = $false
if ($profile -and (Test-Path -LiteralPath $envPath)) {
    Import-WorktreeEnvFile -RepoRoot $RepoRoot
    if (-not $PSBoundParameters.ContainsKey('BackendPort')) {
        $BackendPort = Get-EnvInt 'BACKEND_PORT' $BackendPort
    }
    if (-not $PSBoundParameters.ContainsKey('FrontendPort')) {
        $FrontendPort = Get-EnvInt 'FRONTEND_PORT' $FrontendPort
    }
    $envOk = ($env:STORAGE_MYSQL_PORT -eq [string]$profile.MysqlPort) -and
             ($env:STORAGE_MYSQL_CONTAINER -eq $profile.MysqlContainer) -and
             ($env:STORAGE_BACKEND_CONTAINER -eq $profile.BackendContainer)
    Write-CheckResult '.env' $envOk "STORAGE_MYSQL_PORT=$($env:STORAGE_MYSQL_PORT) mysql=$($env:STORAGE_MYSQL_CONTAINER) backend=$($env:STORAGE_BACKEND_CONTAINER)"
} else {
    Write-CheckResult '.env' $false "missing or no profile; run sync-worktree-env.ps1"
}

if ($profile) {
    $mysqlRunning = Test-ContainerRunning $profile.MysqlContainer
    $minioRunning = Test-ContainerRunning $profile.MinioContainer
    $backendRunning = Test-ContainerRunning $profile.BackendContainer
    Write-CheckResult 'mysql-container' $mysqlRunning $profile.MysqlContainer
    Write-CheckResult 'minio-container' $minioRunning $profile.MinioContainer
    Write-CheckResult 'backend-container' $backendRunning $profile.BackendContainer

    $legacyMysql = docker ps -a --filter "name=^/material-ledger-mysql$" --format "{{.Names}}" 2>$null
    $legacyMinio = docker ps -a --filter "name=^/material-ledger-minio$" --format "{{.Names}}" 2>$null
    $noLegacy = -not $legacyMysql -and -not $legacyMinio
    if (-not $noLegacy) {
        Write-CheckResult 'legacy-docker' $false "run cleanup-legacy-docker.ps1 (material-ledger-* still exists)"
    } else {
        Write-CheckResult 'legacy-docker' $true "no material-ledger-* containers"
    }

    if ($mysqlRunning) {
        $mysqlUser = Get-EnvText 'MYSQL_USER' 'storage'
        $mysqlPassword = Get-EnvText 'MYSQL_PASSWORD' 'storage123'
        $mysqlDb = Get-EnvText 'MYSQL_DB' 'storage'
        $mysqlArgs = @(
            'exec', '-e', "MYSQL_PWD=$mysqlPassword", $profile.MysqlContainer,
            'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
            $mysqlDb, '-N', '-e', "SELECT name FROM sys_menu WHERE id = 111 LIMIT 1;"
        )
        $sample = docker @mysqlArgs 2>$null
        $chineseOk = $LASTEXITCODE -eq 0 -and $sample -and ($sample -notmatch '\?')
        Write-CheckResult 'mysql-chinese' $chineseOk "sample=$sample"
    } else {
        Write-CheckResult 'mysql-chinese' $false "mysql container not running"
    }

    if ($minioRunning -and $backendRunning) {
        $backendKey = (docker exec $profile.BackendContainer printenv MINIO_ACCESS_KEY 2>$null).Trim()
        $minioKey = (docker exec $profile.MinioContainer printenv MINIO_ROOT_USER 2>$null).Trim()
        $keysMatch = [bool]$backendKey -and ($backendKey -eq $minioKey)
        Write-CheckResult 'minio-credentials' $keysMatch "backend=$backendKey minio=$minioKey"

        $minioHostPort = Get-EnvInt 'STORAGE_MINIO_PORT' $profile.MinioPort
        $minioLive = $false
        try {
            Invoke-WebRequest -Uri "http://127.0.0.1:$minioHostPort/minio/health/live" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop | Out-Null
            $minioLive = $true
        } catch {
            $minioLive = $false
        }
        Write-CheckResult 'minio-live' $minioLive "http://127.0.0.1:$minioHostPort/minio/health/live"

        $minioReachableFromBackend = $false
        $reachOutput = docker exec $profile.BackendContainer curl -fsS "http://minio:9000/minio/health/live" 2>$null
        if ($LASTEXITCODE -eq 0) {
            $minioReachableFromBackend = $true
        }
        Write-CheckResult 'minio-from-backend' $minioReachableFromBackend "http://minio:9000/minio/health/live"
    } else {
        Write-CheckResult 'minio-credentials' $false "backend or minio container not running"
        Write-CheckResult 'minio-live' $false "minio container not running"
        Write-CheckResult 'minio-from-backend' $false "backend or minio container not running"
    }
}

$backendReady = $false
if (Test-PortInUse $BackendPort) {
    try {
        Invoke-WebRequest -Uri "http://localhost:$BackendPort/api/auth/me" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop | Out-Null
        $backendReady = $true
    } catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -eq 401 -or $statusCode -eq 403) {
            $backendReady = $true
        }
    }
}
Write-CheckResult 'backend' $backendReady "http://localhost:$BackendPort"

$frontendReady = Test-PortInUse $FrontendPort
Write-CheckResult 'frontend' $frontendReady "http://localhost:$FrontendPort"

Write-Host ""
if ($failures -eq 0) {
    Write-Host "All checks passed." -ForegroundColor Green
    exit 0
}

Write-Host "$failures check(s) failed." -ForegroundColor Red
exit 1
