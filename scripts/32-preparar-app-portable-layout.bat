@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "APP_NAME=DocuPodcastStudio"
set "APP_IMAGE=dist\app-image\%APP_NAME%"
set "PORTABLE_DIR=dist\portable\%APP_NAME%"
set "MANIFEST=dist\portable\PORTABLE_MANIFEST.txt"

if not exist "%APP_IMAGE%" (
  echo ERROR: No existe app-image en %APP_IMAGE%. Ejecuta scripts\14-app-image-completa.bat primero.
  popd >nul
  exit /b 1
)

if exist "%PORTABLE_DIR%" rmdir /s /q "%PORTABLE_DIR%"
mkdir "dist\portable" >nul 2>nul
xcopy /e /i /y "%APP_IMAGE%" "%PORTABLE_DIR%" >nul

if exist "tools" xcopy /e /i /y "tools" "%PORTABLE_DIR%\tools" >nul
if exist "models" xcopy /e /i /y "models" "%PORTABLE_DIR%\models" >nul
if exist "scripts\tts" xcopy /e /i /y "scripts\tts" "%PORTABLE_DIR%\scripts\tts" >nul
if exist "dist\legal" xcopy /e /i /y "dist\legal" "%PORTABLE_DIR%\legal" >nul
if exist "packaging\windows" xcopy /e /i /y "packaging\windows" "%PORTABLE_DIR%\branding" >nul

(
  echo @echo off
  echo setlocal EnableExtensions
  echo set "DOCUPODCAST_APP_ROOT=%%~dp0"
  echo start "" "%%~dp0%APP_NAME%.exe"
  echo exit /b 0
) > "%PORTABLE_DIR%\run-docupodcast-studio.bat"
if not exist "%PORTABLE_DIR%\run-docupodcast-studio.bat" (
  echo ERROR: No se pudo crear el launcher portable.
  popd >nul
  exit /b 1
)

(
  echo DocuPodcast Studio - PF4A Portable Manifest
  echo Fecha: %DATE% %TIME%
  echo App image origen: %APP_IMAGE%
  echo Carpeta portable: %PORTABLE_DIR%
  echo Incluye tools si existian en la raiz.
  echo Incluye models si existian en la raiz.
  echo Incluye scripts\tts si existian en la raiz.
  echo Incluye legal si se genero TP4.
  echo Incluye branding\docupodcast-icon.ico y branding\docupodcast-icon.png para lanzador/instalador.
  echo Launcher: %PORTABLE_DIR%\run-docupodcast-studio.bat
  echo Runtime root esperado al ejecutar portable: carpeta de la app mediante DOCUPODCAST_APP_ROOT.
) > "%MANIFEST%"

echo [DocuPodcast Studio] Carpeta portable preparada: %PORTABLE_DIR%
echo [DocuPodcast Studio] Manifest: %MANIFEST%
popd >nul
exit /b 0
