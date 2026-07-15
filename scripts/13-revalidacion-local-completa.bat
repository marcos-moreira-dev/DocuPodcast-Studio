@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Revalidacion local completa desde: %CD%
echo.

call scripts\00-verificar-entorno.bat
if errorlevel 1 goto :fail

call scripts\03-verificar-toolchain.bat
if errorlevel 1 goto :fail

call scripts\04-verificar-tts-config.bat
if errorlevel 1 goto :fail

echo.
echo [DocuPodcast Studio] Ejecutando clean test package...
call mvn clean test package
if errorlevel 1 goto :fail

echo.
echo [DocuPodcast Studio] Revalidacion local completa OK.
popd >nul
exit /b 0

:fail
echo.
echo [DocuPodcast Studio] Revalidacion local completa FALLO.
popd >nul
exit /b 1
