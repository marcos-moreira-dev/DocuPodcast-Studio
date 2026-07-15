@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)
echo [DocuPodcast Studio] Ejecutando tests desde: %CD%
echo.
call mvn test
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] Tests FALLARON. Revisa target\surefire-reports.
  popd >nul
  exit /b %EXIT_CODE%
)
echo.
echo [DocuPodcast Studio] Tests OK.
popd >nul
exit /b 0
