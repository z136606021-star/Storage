# 重置本 worktree 的 MySQL（material-ledger-mysql，端口 3307）
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host "Stopping worktree MySQL and removing volume..."
Set-Location $Root
docker compose down -v

Write-Host "Starting fresh MySQL on port 3307..."
docker compose up -d

Write-Host "Waiting for MySQL to initialize..."
Start-Sleep -Seconds 20

$count = docker exec material-ledger-mysql mysql -ustorage -pstorage123 --default-character-set=utf8mb4 storage -N -e "SELECT COUNT(*) FROM material_ledger;" 2>$null
Write-Host "material_ledger rows: $count"

docker exec material-ledger-mysql mysql -ustorage -pstorage123 --default-character-set=utf8mb4 storage -e "SELECT id, category, name FROM material_ledger LIMIT 3;" 2>$null

Write-Host "Done. Restart backend: cd backend; mvn spring-boot:run"
