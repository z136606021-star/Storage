@echo off
setlocal
cd /d "%~dp0"
echo Starting dev services...
echo Tip: pass -NoOpenBrowser to skip opening the browser.
set "DEPLOY_ARGS=%*"
echo %DEPLOY_ARGS% | findstr /I /C:"-Build" >nul
if errorlevel 1 (
  echo.
  echo The dev frontend is served from a Docker image. Rebuild it to see latest code changes.
  choice /C YN /N /M "Rebuild Docker images now? [Y/N] "
  if errorlevel 2 (
    echo Skipping rebuild. Existing images will be reused.
  ) else (
    set "DEPLOY_ARGS=-Build %DEPLOY_ARGS%"
  )
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\deploy-cli.ps1" -Profile dev %DEPLOY_ARGS%
if errorlevel 1 (
  echo.
  echo Failed to start dev services.
  echo Check Docker Desktop is running, network access, and base image pull status.
  pause
  exit /b 1
)
echo.
echo Dev services are running.
pause
