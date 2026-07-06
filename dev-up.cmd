@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\deploy-cli.ps1" -Profile dev %*
if errorlevel 1 (
  echo.
  echo Failed to start dev environment.
  pause
  exit /b 1
)
echo.
pause
