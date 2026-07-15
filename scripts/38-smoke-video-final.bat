@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0.."
echo [DocuPodcast Studio] Smoke de video final MP4

echo Este smoke valida que el flujo MP4 final este protegido por readiness antes de renderizar.
echo Para ejecutar FFmpeg real, usa tambien scripts\99-diagnostico-completo.bat --real-engines.

call mvn -Dtest=VideoSmokeRc1SourceTest test
if errorlevel 1 (
  echo [DocuPodcast Studio] Smoke de video final FALLO.
  exit /b 1
)

echo [DocuPodcast Studio] Smoke de video final OK.
exit /b 0
