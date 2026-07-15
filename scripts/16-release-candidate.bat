@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "RC_DIR=dist\release-candidate"
if not exist "%RC_DIR%" mkdir "%RC_DIR%" >nul

echo [DocuPodcast Studio] Preparando release candidate desde: %CD%
echo.

call scripts\13-revalidacion-local-completa.bat
if errorlevel 1 goto :fail

call scripts\29-verificar-runtime-layout.bat
if errorlevel 1 goto :fail

call scripts\30-generar-manifest-terceros.bat
if errorlevel 1 goto :fail

call scripts\14-app-image-completa.bat
if errorlevel 1 goto :fail

call scripts\32-preparar-app-portable-layout.bat
if errorlevel 1 goto :fail

call scripts\33-smoke-rc-instalable.bat
if errorlevel 1 goto :fail

call scripts\34-smoke-app-portable-runtime.bat
if errorlevel 1 goto :fail

call scripts\35-auditar-artefactos-motores.bat
if errorlevel 1 goto :fail

call scripts\36-smoke-gui-asistido.bat
if errorlevel 1 goto :fail

(
  echo DocuPodcast Studio - Release Candidate Manifest
  echo Fecha: %DATE% %TIME%
  echo Estado: RC generado localmente
  echo Validacion automatica: scripts\13-revalidacion-local-completa.bat OK
  echo App-image: dist\app-image\DocuPodcastStudio
  echo Portable: dist\portable\DocuPodcastStudio
  echo Runtime layout: target\runtime-layout\TP3_RUNTIME_LAYOUT_REPORT.md
  echo Third-party manifest: target\legal\THIRD_PARTY_MANIFEST.md
  echo RC smoke: dist\release-candidate\TP6_RC_SMOKE_REPORT.md
  echo PF4B portable smoke: dist\release-candidate\PF4B_PORTABLE_RUNTIME_SMOKE_REPORT.md
  echo PF5A engine audit: target\legal\ENGINE_ARTIFACTS_AUDIT.md
  echo PF6B GUI smoke checklist: dist\release-candidate\PF6B_GUI_SMOKE_CHECKLIST.md
  echo PF5B/PF6A branding: packaging\windows\docupodcast-icon.ico aplicado a app-image/MSI/portable
  echo Pendiente obligatorio: smoke manual documentado en docs\testeo\SMOKE_MANUAL_RELEASE_CANDIDATE.md
  echo Pendiente opcional: MSI con scripts\15-msi-completo.bat si WiX/jpackage MSI estan disponibles
) > "%RC_DIR%\RELEASE_CANDIDATE_MANIFEST.txt"

echo.
echo [DocuPodcast Studio] Release candidate preparado en %RC_DIR%
popd >nul
exit /b 0

:fail
echo.
echo [DocuPodcast Studio] No se pudo preparar el release candidate.
popd >nul
exit /b 1
