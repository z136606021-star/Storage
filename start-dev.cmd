@echo off
setlocal
cd /d "%~dp0"

echo.
echo Storage local dev launcher
echo   - Frontend: Vite dev server with hot reload
echo   - Backend:  mvn spring-boot:run
echo   - Docker:   only MySQL + MinIO
echo.
echo Tip: pass -NoOpenBrowser to skip opening the browser.
echo Tip: for Docker-only deploy without HMR, use dev-up.cmd instead.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-dev-local.ps1" %*
if errorlevel 1 (
  echo.
  echo Failed to start local dev environment.
  echo Check Docker Desktop, Java/Maven, Node.js, and .env sync.
  pause
  exit /b 1
)

echo.
pause
