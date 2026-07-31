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

REM Use JDK 17 for this launcher session only; leave system Java 8 as default.
call :resolve_java17
if errorlevel 1 (
  echo.
  echo Failed to find Java 17.
  echo Install JDK 17, or set STORAGE_JAVA_HOME to your JDK 17 path.
  echo Example:
  echo   set STORAGE_JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
  echo System Java 8 is unchanged; this script only switches for local dev.
  pause
  exit /b 1
)
echo Using JAVA_HOME=%JAVA_HOME%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-dev-local.ps1" %*
if errorlevel 1 (
  echo.
  echo Failed to start local dev environment.
  echo Check Docker Desktop, Java 17/Maven, Node.js, and .env sync.
  pause
  exit /b 1
)

echo.
pause
exit /b 0

:resolve_java17
REM Optional override for this machine without touching system JAVA_HOME.
if defined STORAGE_JAVA_HOME (
  if exist "%STORAGE_JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=%STORAGE_JAVA_HOME%"
    goto :apply_java_home
  )
)

REM Keep current JAVA_HOME when it is already JDK 17.
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" (
    call :is_java17 "%JAVA_HOME%"
    if not errorlevel 1 goto :apply_java_home
  )
)

REM Common Windows JDK 17 install locations.
for /d %%D in (
  "C:\Program Files\Eclipse Adoptium\jdk-17*"
  "C:\Program Files\Microsoft\jdk-17*"
  "C:\Program Files\Java\jdk-17*"
  "C:\Program Files\Amazon Corretto\jdk17*"
  "C:\Program Files\Temurin\jdk-17*"
) do (
  if exist "%%~fD\bin\java.exe" (
    set "JAVA_HOME=%%~fD"
    goto :apply_java_home
  )
)

exit /b 1

:apply_java_home
set "PATH=%JAVA_HOME%\bin;%PATH%"
call :is_java17 "%JAVA_HOME%"
if errorlevel 1 (
  echo Found JAVA_HOME but it is not Java 17: %JAVA_HOME%
  exit /b 1
)
exit /b 0

:is_java17
if not exist "%~1\bin\java.exe" exit /b 1
REM Prefer JDK release file: JAVA_VERSION="17.x.y"
if exist "%~1\release" (
  findstr /B /R /C:"JAVA_VERSION=.17" "%~1\release" >nul
  exit /b %ERRORLEVEL%
)
REM Fallback: capture java -version to a temp file (avoids findstr quote issues).
"%~1\bin\java.exe" -version >"%TEMP%\storage-java-version.txt" 2>&1
findstr /C:"17." "%TEMP%\storage-java-version.txt" >nul
exit /b %ERRORLEVEL%
