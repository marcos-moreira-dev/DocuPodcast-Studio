@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)
if not defined DOCUPODCAST_APP_HEAP set "DOCUPODCAST_APP_HEAP=-Xmx2048m"
echo [DocuPodcast Studio] Ejecutando app desde: %CD%
echo [DocuPodcast Studio] Heap de app: %DOCUPODCAST_APP_HEAP%
call mvn -q javafx:run -Ddocupodcast.app.heap=%DOCUPODCAST_APP_HEAP%
set "EXIT_CODE=%ERRORLEVEL%"
popd >nul
exit /b %EXIT_CODE%
