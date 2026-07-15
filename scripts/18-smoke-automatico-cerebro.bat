@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Smoke automatico del cerebro T79

echo Ejecutando solo BrainSmokeScenarioTest. Para suite completa usa scripts\02-ejecutar-tests.bat
call mvn -Dtest=BrainSmokeScenarioTest test
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] Smoke automatico del cerebro FALLO. Revisa target\surefire-reports y target\docupodcast-smoke.
  popd >nul
  exit /b %EXIT_CODE%
)

echo.
echo [DocuPodcast Studio] Smoke automatico OK.
echo Evidencia esperada:
echo   target\docupodcast-smoke\SMOKE_REPORT.md
echo   target\docupodcast-smoke\PROJECT_INTEGRITY.md
echo   target\docupodcast-smoke\EXPORT_READINESS.md
echo   target\docupodcast-smoke\project-tree.txt
popd >nul
exit /b 0
