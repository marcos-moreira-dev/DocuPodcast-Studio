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
set "DEST_DIR=dist\app-image"
set "MAIN_MODULE=com.marcosmoreiradev.docupodcaststudio/com.marcosmoreiradev.docupodcaststudio.DocuPodcastStudioApp"
set "ICON_ICO=packaging\windows\docupodcast-icon.ico"
set "ICON_PNG=packaging\windows\docupodcast-icon.png"

echo [DocuPodcast Studio] Generando app-image desde: %CD%

if not exist "%ICON_ICO%" (
  echo ERROR: No existe icono de producto en %ICON_ICO%.
  goto :fail
)

echo.
echo [1/4] Compilando artefacto...
call mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="%DEPENDENCY_DIR%"
if errorlevel 1 goto :fail

if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%" >nul
if not exist "%DEST_DIR%" mkdir "%DEST_DIR%" >nul

copy /y "target\docupodcast-studio-0.0.1-onboarding.jar" "%INPUT_DIR%\" >nul
if exist "%DEPENDENCY_DIR%" xcopy /e /i /y "%DEPENDENCY_DIR%" "%INPUT_DIR%\lib" >nul

echo.
echo [2/6] Verificando jpackage...
where jpackage >nul 2>nul
if errorlevel 1 (
  echo ERROR: jpackage no esta disponible en PATH. Usa JDK 21 completo, no solo JRE.
  goto :fail
)

echo.
echo [3/6] Ejecutando jpackage app-image...
if exist "%DEST_DIR%\%APP_NAME%" rmdir /s /q "%DEST_DIR%\%APP_NAME%"
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Omar Alvarado" ^
  --description "DocuPodcast Studio" ^
  --dest "%DEST_DIR%" ^
  --module-path "%INPUT_DIR%;%INPUT_DIR%\lib" ^
  --module "%MAIN_MODULE%" ^
  --icon "%ICON_ICO%"
if errorlevel 1 goto :fail

echo.
echo [4/6] Copiando recursos runtime y legales...
if exist "tools" xcopy /e /i /y "tools" "%DEST_DIR%\%APP_NAME%\tools" >nul
if exist "models" xcopy /e /i /y "models" "%DEST_DIR%\%APP_NAME%\models" >nul
if exist "scripts\tts" xcopy /e /i /y "scripts\tts" "%DEST_DIR%\%APP_NAME%\scripts\tts" >nul
if exist "dist\legal" xcopy /e /i /y "dist\legal" "%DEST_DIR%\%APP_NAME%\legal" >nul
if exist "packaging\windows" xcopy /e /i /y "packaging\windows" "%DEST_DIR%\%APP_NAME%\branding" >nul

echo.
echo [5/6] Generando launcher portable...
(
  echo @echo off
  echo setlocal EnableExtensions
  echo set "DOCUPODCAST_APP_ROOT=%%~dp0"
  echo start "" "%%~dp0%APP_NAME%.exe"
  echo exit /b 0
) > "%DEST_DIR%\%APP_NAME%\run-docupodcast-studio.bat"
if not exist "%DEST_DIR%\%APP_NAME%\run-docupodcast-studio.bat" goto :fail

echo.
echo [6/6] Generando manifest de app-image...
(
  echo DocuPodcast Studio - App Image Manifest
  echo Fecha: %DATE% %TIME%
  echo Carpeta: %DEST_DIR%\%APP_NAME%
  echo Java objetivo: 21 Temurin via Maven Toolchain
  echo TTS: Configuracion in-app de Voz IA avanzada, Voz local simple o Modo de prueba diagnostico
  echo Runtime: tools/models/scripts copiados si existian
  echo Icono de producto: %ICON_ICO% integrado en jpackage y branding copiado a la app-image
  echo Legal: dist\legal copiado si existia
  echo Launcher: %DEST_DIR%\%APP_NAME%\run-docupodcast-studio.bat define DOCUPODCAST_APP_ROOT para doble clic limpio
) > "%DEST_DIR%\APP_IMAGE_MANIFEST.txt"

echo.
echo [DocuPodcast Studio] App-image generado en %DEST_DIR%\%APP_NAME%
popd >nul
exit /b 0

:fail
echo.
echo [DocuPodcast Studio] Fallo la generacion de app-image.
popd >nul
exit /b 1
