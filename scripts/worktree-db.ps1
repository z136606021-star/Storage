# Worktree 数据库隔离注册表（SSOT）
# 供 sync-worktree-env.ps1、deploy-cli.ps1 使用

$script:WorktreeDbProfiles = @(
    @{
        Branch              = 'main'
        Slug                = 'main'
        WorktreePath        = 'E:/Storage'
        ComposeProjectName  = 'storage-main'
        MysqlPort           = 3307
        MinioPort           = 9000
        MysqlContainer      = 'storage-main-mysql'
        MinioContainer      = 'storage-main-minio'
        BackendContainer    = 'storage-main-backend'
        FrontendContainer   = 'storage-main-frontend'
        MinioConsolePort    = 9001
        MysqlVolume         = 'storage-main_mysql_data'
        MinioVolume         = 'storage-main_minio_data'
    }
    @{
        Branch              = 'feat/material-ledger'
        Slug                = 'material-ledger'
        WorktreePath        = 'E:/Storage-worktrees/material-ledger'
        ComposeProjectName  = 'storage-material-ledger'
        MysqlPort           = 3308
        MinioPort           = 9010
        MysqlContainer      = 'storage-material-ledger-mysql'
        MinioContainer      = 'storage-material-ledger-minio'
        BackendContainer    = 'storage-material-ledger-backend'
        FrontendContainer   = 'storage-material-ledger-frontend'
        MinioConsolePort    = 9011
        MysqlVolume         = 'storage-material-ledger_mysql_data'
        MinioVolume         = 'storage-material-ledger_minio_data'
    }
    @{
        Branch              = 'feat/material-io'
        Slug                = 'material-io'
        WorktreePath        = 'E:/Storage-worktrees/material-io'
        ComposeProjectName  = 'storage-material-io'
        MysqlPort           = 3309
        MinioPort           = 9020
        MysqlContainer      = 'storage-material-io-mysql'
        MinioContainer      = 'storage-material-io-minio'
        BackendContainer    = 'storage-material-io-backend'
        FrontendContainer   = 'storage-material-io-frontend'
        MinioConsolePort    = 9021
        MysqlVolume         = 'storage-material-io_mysql_data'
        MinioVolume         = 'storage-material-io_minio_data'
    }
    @{
        Branch              = 'feat/safety-stock'
        Slug                = 'safety-stock'
        WorktreePath        = 'E:/Storage-worktrees/safety-stock'
        ComposeProjectName  = 'storage-safety-stock'
        MysqlPort           = 3310
        MinioPort           = 9030
        MysqlContainer      = 'storage-safety-stock-mysql'
        MinioContainer      = 'storage-safety-stock-minio'
        BackendContainer    = 'storage-safety-stock-backend'
        FrontendContainer   = 'storage-safety-stock-frontend'
        MinioConsolePort    = 9031
        MysqlVolume         = 'storage-safety-stock_mysql_data'
        MinioVolume         = 'storage-safety-stock_minio_data'
    }
    @{
        Branch              = 'feat/config-mgmt'
        Slug                = 'config-mgmt'
        WorktreePath        = 'E:/Storage-worktrees/config-mgmt'
        ComposeProjectName  = 'storage-config-mgmt'
        MysqlPort           = 3311
        MinioPort           = 9040
        MysqlContainer      = 'storage-config-mgmt-mysql'
        MinioContainer      = 'storage-config-mgmt-minio'
        BackendContainer    = 'storage-config-mgmt-backend'
        FrontendContainer   = 'storage-config-mgmt-frontend'
        MinioConsolePort    = 9041
        MysqlVolume         = 'storage-config-mgmt_mysql_data'
        MinioVolume         = 'storage-config-mgmt_minio_data'
    }
    @{
        Branch              = 'feat/knowledge-base'
        Slug                = 'knowledge-base'
        WorktreePath        = 'E:/Storage-worktrees/knowledge-base'
        ComposeProjectName  = 'storage-knowledge-base'
        MysqlPort           = 3312
        MinioPort           = 9050
        MysqlContainer      = 'storage-knowledge-base-mysql'
        MinioContainer      = 'storage-knowledge-base-minio'
        BackendContainer    = 'storage-knowledge-base-backend'
        FrontendContainer   = 'storage-knowledge-base-frontend'
        MinioConsolePort    = 9051
        MysqlVolume         = 'storage-knowledge-base_mysql_data'
        MinioVolume         = 'storage-knowledge-base_minio_data'
    }
    @{
        Branch              = 'feat/design-guidelines'
        Slug                = 'design-guidelines'
        WorktreePath        = 'E:/Storage-worktrees/design-guidelines'
        ComposeProjectName  = 'storage-design-guidelines'
        MysqlPort           = 3313
        MinioPort           = 9060
        MysqlContainer      = 'storage-design-guidelines-mysql'
        MinioContainer      = 'storage-design-guidelines-minio'
        BackendContainer    = 'storage-design-guidelines-backend'
        FrontendContainer   = 'storage-design-guidelines-frontend'
        MinioConsolePort    = 9061
        MysqlVolume         = 'storage-design-guidelines_mysql_data'
        MinioVolume         = 'storage-design-guidelines_minio_data'
    }
)

function Get-WorktreeDbProfile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Branch
    )

    $profile = $script:WorktreeDbProfiles | Where-Object { $_.Branch -eq $Branch } | Select-Object -First 1
    if (-not $profile) {
        $known = ($script:WorktreeDbProfiles | ForEach-Object { $_.Branch }) -join ', '
        throw "Unknown branch '$Branch'. Known branches: $known"
    }
    return $profile
}

function Get-CurrentBranchName {
    param(
        [string]$RepoRoot = (Get-Location).Path
    )

    $branch = git -C $RepoRoot branch --show-current 2>$null
    if (-not $branch) {
        throw "Not a git repository or detached HEAD: $RepoRoot"
    }
    return $branch.Trim()
}

function Get-CurrentBranchProfile {
    param(
        [string]$RepoRoot = (Get-Location).Path
    )

    $branch = Get-CurrentBranchName -RepoRoot $RepoRoot
    $profile = Get-WorktreeDbProfile -Branch $branch

    $normalizedRoot = (Resolve-Path -LiteralPath $RepoRoot).Path -replace '\\', '/'
    $normalizedExpected = $profile.WorktreePath -replace '\\', '/'
    if ($normalizedRoot -ne $normalizedExpected) {
        Write-Warning "Current path ($normalizedRoot) does not match registered worktree for branch '$branch' ($normalizedExpected). Using branch profile anyway."
    }

    return $profile
}

function Format-WorktreeEnvContent {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Profile,
        [string]$MysqlDb = 'storage',
        [string]$MysqlUser = 'storage',
        [string]$MysqlPassword = 'storage123',
        [string]$MysqlRootPassword = 'root123',
        [string]$MinioAccessKey = 'minioadmin',
        [string]$MinioSecretKey = 'minioadmin123',
        [string]$MinioBucket = 'storage',
        [string]$BackendPort = '8080',
        [string]$FrontendPort = '5173',
        [string]$ViteApiProxy = '',
        [string]$SessionCookieHttpOnly = 'true',
        [string]$SessionCookieSecure = 'false',
        [string]$ResetAdminPasswordOnStartup = 'true',
        [string]$JwtSecret = 'dev-only-change-this-jwt-secret-at-least-32-bytes',
        [string]$JwtTtlMinutes = '120',
        [string]$UploadMaxSizeBytes = '5368709120',
        [string]$UploadMaxRequestSizeBytes = '5505025024',
        [string]$UploadMaxFilesPerRecord = '20',
        [string]$AppPublicBaseUrl = '',
        [string]$PasswordResetTokenTtlMinutes = '30',
        [string]$MailHost = 'smtp.gmail.com',
        [string]$MailPort = '587',
        [string]$MailUsername = '',
        [string]$MailPassword = '',
        [string]$MailFrom = '',
        [string]$MailSmtpAuth = 'true',
        [string]$MailSmtpStarttlsEnable = 'true'
    )

    $minioEndpoint = "http://minio:9000"
    if ([string]::IsNullOrWhiteSpace($ViteApiProxy)) {
        $ViteApiProxy = "http://localhost:$BackendPort"
    }
    if ([string]::IsNullOrWhiteSpace($AppPublicBaseUrl)) {
        $AppPublicBaseUrl = "http://localhost"
    }
    return @"
# Auto-generated by scripts/sync-worktree-env.ps1 - do not commit (.gitignore)
# Branch: $($Profile.Branch) | Compose: $($Profile.ComposeProjectName)
# Existing credentials, app ports, API proxy, JWT, upload, and mail values are preserved when this file is regenerated.

MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DB=$MysqlDb
MYSQL_USER=$MysqlUser
MYSQL_PASSWORD=$MysqlPassword
MYSQL_ROOT_PASSWORD=$MysqlRootPassword

MINIO_ENDPOINT=$minioEndpoint
MINIO_ACCESS_KEY=$MinioAccessKey
MINIO_SECRET_KEY=$MinioSecretKey
MINIO_BUCKET=$MinioBucket

BACKEND_PORT=$BackendPort
FRONTEND_PORT=$FrontendPort
APP_PORT=80
VITE_API_PROXY=$ViteApiProxy
SESSION_COOKIE_HTTP_ONLY=$SessionCookieHttpOnly
SESSION_COOKIE_SECURE=$SessionCookieSecure
RESET_ADMIN_PASSWORD_ON_STARTUP=$ResetAdminPasswordOnStartup
JWT_SECRET=$JwtSecret
JWT_TTL_MINUTES=$JwtTtlMinutes
UPLOAD_MAX_SIZE_BYTES=$UploadMaxSizeBytes
UPLOAD_MAX_REQUEST_SIZE_BYTES=$UploadMaxRequestSizeBytes
UPLOAD_MAX_FILES_PER_RECORD=$UploadMaxFilesPerRecord

APP_PUBLIC_BASE_URL=$AppPublicBaseUrl
PASSWORD_RESET_TOKEN_TTL_MINUTES=$PasswordResetTokenTtlMinutes
MAIL_HOST=$MailHost
MAIL_PORT=$MailPort
MAIL_USERNAME=$MailUsername
MAIL_PASSWORD=$MailPassword
MAIL_FROM=$MailFrom
MAIL_SMTP_AUTH=$MailSmtpAuth
MAIL_SMTP_STARTTLS_ENABLE=$MailSmtpStarttlsEnable

COMPOSE_PROJECT_NAME=$($Profile.ComposeProjectName)
STORAGE_MYSQL_PORT=$($Profile.MysqlPort)
STORAGE_MINIO_PORT=$($Profile.MinioPort)
STORAGE_MINIO_CONSOLE_PORT=$($Profile.MinioConsolePort)
STORAGE_MYSQL_CONTAINER=$($Profile.MysqlContainer)
STORAGE_MINIO_CONTAINER=$($Profile.MinioContainer)
STORAGE_BACKEND_CONTAINER=$($Profile.BackendContainer)
STORAGE_FRONTEND_CONTAINER=$($Profile.FrontendContainer)
STORAGE_MYSQL_VOLUME=$($Profile.MysqlVolume)
STORAGE_MINIO_VOLUME=$($Profile.MinioVolume)
"@
}

function Read-DotEnvValues {
    param(
        [string]$EnvPath
    )

    $values = @{}
    if (-not (Test-Path -LiteralPath $EnvPath)) {
        return $values
    }

    Get-Content -LiteralPath $EnvPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#')) {
            $eq = $line.IndexOf('=')
            if ($eq -gt 0) {
                $name = $line.Substring(0, $eq).Trim()
                $value = $line.Substring($eq + 1).Trim()
                $values[$name] = $value
            }
        }
    }

    return $values
}

function Get-EnvOrExistingValue {
    param(
        [hashtable]$Existing,
        [string]$Name,
        [string]$DefaultValue
    )

    $envValue = (Get-Item -Path "Env:$Name" -ErrorAction SilentlyContinue).Value
    if (-not [string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue
    }
    if ($Existing.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($Existing[$Name])) {
        return $Existing[$Name]
    }
    return $DefaultValue
}

function Write-WorktreeEnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Profile,
        [string]$RepoRoot = (Get-Location).Path
    )

    $envPath = Join-Path $RepoRoot '.env'
    $existing = Read-DotEnvValues -EnvPath $envPath
    $mysqlDb = Get-EnvOrExistingValue -Existing $existing -Name 'MYSQL_DB' -DefaultValue 'storage'
    $mysqlUser = Get-EnvOrExistingValue -Existing $existing -Name 'MYSQL_USER' -DefaultValue 'storage'
    $mysqlPassword = Get-EnvOrExistingValue -Existing $existing -Name 'MYSQL_PASSWORD' -DefaultValue 'storage123'
    $mysqlRootPassword = Get-EnvOrExistingValue -Existing $existing -Name 'MYSQL_ROOT_PASSWORD' -DefaultValue 'root123'
    $minioAccessKey = Get-EnvOrExistingValue -Existing $existing -Name 'MINIO_ACCESS_KEY' -DefaultValue 'minioadmin'
    $minioSecretKey = Get-EnvOrExistingValue -Existing $existing -Name 'MINIO_SECRET_KEY' -DefaultValue 'minioadmin123'
    $minioBucket = Get-EnvOrExistingValue -Existing $existing -Name 'MINIO_BUCKET' -DefaultValue 'storage'
    $backendPort = Get-EnvOrExistingValue -Existing $existing -Name 'BACKEND_PORT' -DefaultValue '8080'
    $frontendPort = Get-EnvOrExistingValue -Existing $existing -Name 'FRONTEND_PORT' -DefaultValue '5173'
    $viteApiProxy = Get-EnvOrExistingValue -Existing $existing -Name 'VITE_API_PROXY' -DefaultValue "http://localhost:$backendPort"
    $sessionCookieHttpOnly = Get-EnvOrExistingValue -Existing $existing -Name 'SESSION_COOKIE_HTTP_ONLY' -DefaultValue 'true'
    $sessionCookieSecure = Get-EnvOrExistingValue -Existing $existing -Name 'SESSION_COOKIE_SECURE' -DefaultValue 'false'
    $resetAdminPasswordOnStartup = Get-EnvOrExistingValue -Existing $existing -Name 'RESET_ADMIN_PASSWORD_ON_STARTUP' -DefaultValue 'true'
    $jwtSecret = Get-EnvOrExistingValue -Existing $existing -Name 'JWT_SECRET' -DefaultValue 'dev-only-change-this-jwt-secret-at-least-32-bytes'
    $jwtTtlMinutes = Get-EnvOrExistingValue -Existing $existing -Name 'JWT_TTL_MINUTES' -DefaultValue '120'
    $uploadMaxSizeBytes = Get-EnvOrExistingValue -Existing $existing -Name 'UPLOAD_MAX_SIZE_BYTES' -DefaultValue '5368709120'
    $uploadMaxRequestSizeBytes = Get-EnvOrExistingValue -Existing $existing -Name 'UPLOAD_MAX_REQUEST_SIZE_BYTES' -DefaultValue '5505025024'
    # Migrate only known historical defaults; preserve custom upload limits.
    if ($uploadMaxSizeBytes -in @('5242880', '52428800')) {
        $uploadMaxSizeBytes = '5368709120'
    }
    if ($uploadMaxRequestSizeBytes -in @('57671680', '55050240')) {
        $uploadMaxRequestSizeBytes = '5505025024'
    }
    $uploadMaxFilesPerRecord = Get-EnvOrExistingValue -Existing $existing -Name 'UPLOAD_MAX_FILES_PER_RECORD' -DefaultValue '20'
    $appPublicBaseUrl = Get-EnvOrExistingValue -Existing $existing -Name 'APP_PUBLIC_BASE_URL' -DefaultValue "http://localhost"
    if ($appPublicBaseUrl -eq "http://localhost:$frontendPort") {
        $appPublicBaseUrl = "http://localhost"
    }
    $passwordResetTokenTtlMinutes = Get-EnvOrExistingValue -Existing $existing -Name 'PASSWORD_RESET_TOKEN_TTL_MINUTES' -DefaultValue '30'
    $mailHost = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_HOST' -DefaultValue 'smtp.gmail.com'
    $mailPort = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_PORT' -DefaultValue '587'
    $mailUsername = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_USERNAME' -DefaultValue ''
    $mailPassword = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_PASSWORD' -DefaultValue ''
    $mailFrom = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_FROM' -DefaultValue ''
    $mailSmtpAuth = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_SMTP_AUTH' -DefaultValue 'true'
    $mailSmtpStarttlsEnable = Get-EnvOrExistingValue -Existing $existing -Name 'MAIL_SMTP_STARTTLS_ENABLE' -DefaultValue 'true'
    $content = Format-WorktreeEnvContent `
        -Profile $Profile `
        -MysqlDb $mysqlDb `
        -MysqlUser $mysqlUser `
        -MysqlPassword $mysqlPassword `
        -MysqlRootPassword $mysqlRootPassword `
        -MinioAccessKey $minioAccessKey `
        -MinioSecretKey $minioSecretKey `
        -MinioBucket $minioBucket `
        -BackendPort $backendPort `
        -FrontendPort $frontendPort `
        -ViteApiProxy $viteApiProxy `
        -SessionCookieHttpOnly $sessionCookieHttpOnly `
        -SessionCookieSecure $sessionCookieSecure `
        -ResetAdminPasswordOnStartup $resetAdminPasswordOnStartup `
        -JwtSecret $jwtSecret `
        -JwtTtlMinutes $jwtTtlMinutes `
        -UploadMaxSizeBytes $uploadMaxSizeBytes `
        -UploadMaxRequestSizeBytes $uploadMaxRequestSizeBytes `
        -UploadMaxFilesPerRecord $uploadMaxFilesPerRecord `
        -AppPublicBaseUrl $appPublicBaseUrl `
        -PasswordResetTokenTtlMinutes $passwordResetTokenTtlMinutes `
        -MailHost $mailHost `
        -MailPort $mailPort `
        -MailUsername $mailUsername `
        -MailPassword $mailPassword `
        -MailFrom $mailFrom `
        -MailSmtpAuth $mailSmtpAuth `
        -MailSmtpStarttlsEnable $mailSmtpStarttlsEnable
    [System.IO.File]::WriteAllText($envPath, $content, [System.Text.UTF8Encoding]::new($false))
    return $envPath
}

function Import-WorktreeEnvFile {
    param(
        [string]$RepoRoot = (Get-Location).Path
    )

    $envPath = Join-Path $RepoRoot '.env'
    if (-not (Test-Path -LiteralPath $envPath)) {
        throw ".env not found at $envPath. Run scripts/sync-worktree-env.ps1 first."
    }

    Get-Content -LiteralPath $envPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#')) {
            $eq = $line.IndexOf('=')
            if ($eq -gt 0) {
                $name = $line.Substring(0, $eq).Trim()
                $value = $line.Substring($eq + 1).Trim()
                Set-Item -Path "Env:$name" -Value $value
            }
        }
    }
}

function Get-DockerComposeArgs {
  param([string]$RepoRoot = (Get-Location).Path)

  $envPath = Join-Path $RepoRoot '.env'
  if (Test-Path -LiteralPath $envPath) {
    return @('--env-file', $envPath)
  }
  return @()
}
