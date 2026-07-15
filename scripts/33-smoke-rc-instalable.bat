@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=dist\release-candidate"
set "REPORT=%REPORT_DIR%\TP6_RC_SMOKE_REPORT.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - TP6 RC smoke
  echo.
  echo Fecha: %DATE% %TIME%
  echo.
  echo ^| Gate ^| Estado esperado ^| Evidencia ^|
  echo ^|---^|---^|---^|
  echo ^| Diagnostico completo ^| Verde ^| `target/diagnostico-completo` ^|
  echo ^| Smoke visual UX ^| Revisado manualmente ^| `docs/productizacion/T112_SMOKE_VISUAL_REPORTE_MANUAL.md` ^|
  echo ^| Runtime layout ^| Verificado ^| `target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md` ^|
  echo ^| Third-party manifest ^| Generado ^| `target/legal/THIRD_PARTY_MANIFEST.md` ^|
  echo ^| App-image portable ^| Generado ^| `dist/app-image/DocuPodcastStudio` ^|
  echo ^| Motores reales ^| Opcional opt-in ^| `scripts/99-diagnostico-completo.bat --real-engines` ^|
) > "%REPORT%"

echo [DocuPodcast Studio] Smoke RC documentado: %REPORT%
popd >nul
exit /b 0
