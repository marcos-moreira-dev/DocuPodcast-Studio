@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Verificando Piper y FFmpeg locales
echo.
echo Politica: solo artefactos dentro de tools\ y models\. No se usa PATH ni instalaciones globales.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\tts\preflight-piper-ffmpeg.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] Piper/FFmpeg requieren preparacion.
  echo Revisa:
  echo   target\docupodcast-engine-setup\T90F_PIPER_FFMPEG_PREFLIGHT_REPORT.md
  popd >nul
  exit /b %EXIT_CODE%
)

echo.
echo [DocuPodcast Studio] Preflight Piper/FFmpeg finalizado.
echo Reporte:
echo   target\docupodcast-engine-setup\T90F_PIPER_FFMPEG_PREFLIGHT_REPORT.md
popd >nul
exit /b 0
