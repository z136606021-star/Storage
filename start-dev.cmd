@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-dev.ps1" %*
if errorlevel 1 (
  echo.
  echo Failed to start dev services.
  pause
  exit /b 1
)
echo.
pause
