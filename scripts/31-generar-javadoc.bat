@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Generando JavaDoc desde: %CD%
call mvn -q javadoc:javadoc
set "EXIT_CODE=%ERRORLEVEL%"
if "%EXIT_CODE%"=="0" (
  echo JavaDoc generado en target\site\apidocs
) else (
  echo ERROR: Fallo la generacion de JavaDoc.
)
popd >nul
exit /b %EXIT_CODE%
