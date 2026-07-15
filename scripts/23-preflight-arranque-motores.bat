@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Preflight de arranque de motores
echo.
echo Verifica el runtime local/autocontenido de Coqui/XTTS y artefactos de media.
echo No usa Python global ni PATH para Coqui.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\tts\preflight-startup-engines.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
echo.
echo Reporte:
echo   target\docupodcast-engine-startup-preflight\T90E_STARTUP_ENGINE_PREFLIGHT_REPORT.md
popd >nul
exit /b %EXIT_CODE%
