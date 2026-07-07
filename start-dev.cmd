@echo off
setlocal
cd /d "%~dp0"
echo Starting dev services with rebuild (frontend/backend images)...
echo Tip: pass -NoOpenBrowser to skip opening the browser.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\deploy-cli.ps1" -Profile dev -Build %*
if errorlevel 1 (
  echo.
  echo Failed to start dev services.
  echo Check Docker Desktop is running, network access, and base image pull status.
  pause
  exit /b 1
)
echo.
echo Dev services are running. If the UI still looks stale, press Ctrl+F5 in the browser.
pause
