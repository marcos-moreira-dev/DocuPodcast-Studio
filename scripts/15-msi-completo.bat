@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "APP_NAME=DocuPodcastStudio"
set "APP_TITLE=DocuPodcast Studio"
set "APP_VERSION=0.0.1"
set "INPUT_DIR=target\jpackage-input"
set "DEPENDENCY_DIR=target\dependency"
set "DEST_DIR=dist\installer"
set "MAIN_MODULE=com.marcosmoreiradev.docupodcaststudio/com.marcosmoreiradev.docupodcaststudio.DocuPodcastStudioApp"
set "ICON_ICO=packaging\windows\docupodcast-icon.ico"

echo [DocuPodcast Studio] Generando MSI desde: %CD%
echo.
if not exist "%ICON_ICO%" (
  echo ERROR: No existe icono de producto en %ICON_ICO%.
  goto :fail
)

echo Nota: jpackage puede requerir WiX Toolset instalado para generar MSI en Windows.
echo.

call mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="%DEPENDENCY_DIR%"
if errorlevel 1 goto :fail

if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%" >nul
if not exist "%DEST_DIR%" mkdir "%DEST_DIR%" >nul

copy /y "target\docupodcast-studio-0.0.1-onboarding.jar" "%INPUT_DIR%\" >nul
if exist "%DEPENDENCY_DIR%" xcopy /e /i /y "%DEPENDENCY_DIR%" "%INPUT_DIR%\lib" >nul

where jpackage >nul 2>nul
if errorlevel 1 (
  echo ERROR: jpackage no esta disponible en PATH. Usa JDK 21 completo, no solo JRE.
  goto :fail
)

if exist "dist\legal" xcopy /e /i /y "dist\legal" "%INPUT_DIR%\legal" >nul
if exist "tools" xcopy /e /i /y "tools" "%INPUT_DIR%\tools" >nul
if exist "models" xcopy /e /i /y "models" "%INPUT_DIR%\models" >nul
if exist "scripts\tts" xcopy /e /i /y "scripts\tts" "%INPUT_DIR%\scripts\tts" >nul

jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Omar Alvarado" ^
  --description "DocuPodcast Studio" ^
  --dest "%DEST_DIR%" ^
  --module-path "%INPUT_DIR%;%INPUT_DIR%\lib" ^
  --module "%MAIN_MODULE%" ^
  --win-menu ^
  --win-shortcut ^
  --icon "%ICON_ICO%"
if errorlevel 1 goto :fail

(
  echo DocuPodcast Studio - MSI Manifest
  echo Fecha: %DATE% %TIME%
  echo Carpeta: %DEST_DIR%
  echo Java objetivo: 21 Temurin via Maven Toolchain
  echo Requiere prueba manual antes de distribuir.
  echo Runtime/legal staged in jpackage input when present.
  echo Icono MSI/app: %ICON_ICO%
) > "%DEST_DIR%\MSI_MANIFEST.txt"

echo.
echo [DocuPodcast Studio] MSI generado en %DEST_DIR%
popd >nul
exit /b 0

:fail
echo.
echo [DocuPodcast Studio] Fallo la generacion del MSI.
echo Si el error menciona WiX, instala WiX Toolset y vuelve a ejecutar.
popd >nul
exit /b 1
