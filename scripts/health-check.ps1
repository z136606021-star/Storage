# Read-only environment health check (exit 0 = all pass, 1 = any fail)
param(
    [ValidateSet('dev', 'prod')]
    [string]$Profile = 'dev',
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

$DevMysqlContainer = 'storage-mysql'
$DevMinioContainer = 'storage-minio'
$DevBackendContainer = 'storage-backend'
$DevMysqlHostPort = 3307
$DevMinioHostPort = 9000

Write-Host "Storage health check (profile: $Profile)"
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
if (Test-Path -LiteralPath $envPath) {
    Import-WorktreeEnvFile -RepoRoot $RepoRoot
    if (-not $PSBoundParameters.ContainsKey('BackendPort')) {
        $BackendPort = Get-EnvInt 'BACKEND_PORT' $BackendPort
    }
    if (-not $PSBoundParameters.ContainsKey('FrontendPort')) {
        if ($Profile -eq 'prod') {
            $FrontendPort = Get-EnvInt 'APP_PORT' 80
        } else {
            $FrontendPort = Get-EnvInt 'FRONTEND_PORT' $FrontendPort
        }
    }
    Write-CheckResult '.env' $true "found at $envPath"
} else {
    Write-CheckResult '.env' $false "missing; run sync-worktree-env.ps1 for local dev or create prod .env manually"
}

$mysqlRunning = Test-ContainerRunning $DevMysqlContainer
$minioRunning = Test-ContainerRunning $DevMinioContainer
$backendRunning = Test-ContainerRunning $DevBackendContainer

if ($Profile -eq 'dev') {
    Write-CheckResult 'mysql-container' $mysqlRunning $DevMysqlContainer
    Write-CheckResult 'minio-container' $minioRunning $DevMinioContainer
} else {
    Write-CheckResult 'mysql-container' $true 'skipped (prod uses external MySQL)'
    Write-CheckResult 'minio-container' $true 'skipped (prod uses external MinIO)'
}
Write-CheckResult 'backend-container' $backendRunning $DevBackendContainer

$legacyMysql = docker ps -a --filter "name=^/material-ledger-mysql$" --format "{{.Names}}" 2>$null
    $legacyMinio = docker ps -a --filter "name=^/material-ledger-minio$" --format "{{.Names}}" 2>$null
    $noLegacy = -not $legacyMysql -and -not $legacyMinio
    if (-not $noLegacy) {
        Write-CheckResult 'legacy-docker' $false "run cleanup-legacy-docker.ps1 (material-ledger-* still exists)"
    } else {
        Write-CheckResult 'legacy-docker' $true "no material-ledger-* containers"
    }

    if ($Profile -eq 'dev' -and $mysqlRunning) {
        $mysqlUser = Get-EnvText 'MYSQL_USER' 'storage'
        $mysqlPassword = Get-EnvText 'MYSQL_PASSWORD' 'storage123'
        $mysqlDb = Get-EnvText 'MYSQL_DB' 'storage'
        $mysqlArgs = @(
            'exec', '-e', "MYSQL_PWD=$mysqlPassword", $DevMysqlContainer,
            'mysql', "-u$mysqlUser", '--default-character-set=utf8mb4',
            $mysqlDb, '-N', '-e', "SELECT name FROM sys_menu WHERE id = 111 LIMIT 1;"
        )
        $sample = docker @mysqlArgs 2>$null
        $chineseOk = $LASTEXITCODE -eq 0 -and $sample -and ($sample -notmatch '\?')
        Write-CheckResult 'mysql-chinese' $chineseOk "sample=$sample"
    } elseif ($Profile -eq 'dev') {
        Write-CheckResult 'mysql-chinese' $false "mysql container not running"
    } elseif ($backendRunning) {
        $mysqlHost = (docker exec $DevBackendContainer printenv MYSQL_HOST 2>$null).Trim()
        $mysqlPort = (docker exec $DevBackendContainer printenv MYSQL_PORT 2>$null).Trim()
        $mysqlConfigured = -not [string]::IsNullOrWhiteSpace($mysqlHost) -and -not [string]::IsNullOrWhiteSpace($mysqlPort)
        Write-CheckResult 'mysql-endpoint' $mysqlConfigured "MYSQL_HOST=$mysqlHost MYSQL_PORT=$mysqlPort"
    } else {
        Write-CheckResult 'mysql-endpoint' $false 'backend container not running'
    }

    if ($Profile -eq 'dev') {
        if ($minioRunning -and $backendRunning) {
            $backendKey = (docker exec $DevBackendContainer printenv MINIO_ACCESS_KEY 2>$null).Trim()
            $minioKey = (docker exec $DevMinioContainer printenv MINIO_ROOT_USER 2>$null).Trim()
            $keysMatch = [bool]$backendKey -and ($backendKey -eq $minioKey)
            Write-CheckResult 'minio-credentials' $keysMatch "backend=$backendKey minio=$minioKey"

            $minioLive = $false
            try {
                Invoke-WebRequest -Uri "http://127.0.0.1:$DevMinioHostPort/minio/health/live" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop | Out-Null
                $minioLive = $true
            } catch {
                $minioLive = $false
            }
            Write-CheckResult 'minio-live' $minioLive "http://127.0.0.1:$DevMinioHostPort/minio/health/live"

            $minioReachableFromBackend = $false
            docker exec $DevBackendContainer curl -fsS "http://minio:9000/minio/health/live" 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $minioReachableFromBackend = $true
            }
            Write-CheckResult 'minio-from-backend' $minioReachableFromBackend "http://minio:9000/minio/health/live"
        } else {
            Write-CheckResult 'minio-credentials' $false "backend or minio container not running"
            Write-CheckResult 'minio-live' $false "minio container not running"
            Write-CheckResult 'minio-from-backend' $false "backend or minio container not running"
        }
    } elseif ($backendRunning) {
        $minioEndpoint = (docker exec $DevBackendContainer printenv MINIO_ENDPOINT 2>$null).Trim()
        $endpointConfigured = -not [string]::IsNullOrWhiteSpace($minioEndpoint)
        Write-CheckResult 'minio-endpoint' $endpointConfigured "MINIO_ENDPOINT=$minioEndpoint"

        $minioReachableFromBackend = $false
        if ($endpointConfigured) {
            $healthUrl = $minioEndpoint.TrimEnd('/') + '/minio/health/live'
            docker exec $DevBackendContainer curl -fsS $healthUrl 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $minioReachableFromBackend = $true
            }
        }
        Write-CheckResult 'minio-from-backend' $minioReachableFromBackend $minioEndpoint
    } else {
        Write-CheckResult 'minio-endpoint' $false 'backend container not running'
        Write-CheckResult 'minio-from-backend' $false 'backend container not running'
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
