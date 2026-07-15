@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "SETTINGS_FILE=%USERPROFILE%\.docupodcast-studio\operational-settings.properties"
set "ENGINE_MODE=mock"
set "DISPLAY_NAME=Motor TTS local"
set "COMMAND_TEMPLATE="
set "MODELS_DIR=models"

if exist "%SETTINGS_FILE%" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%SETTINGS_FILE%") do (
    if /I "%%A"=="tts.engineMode" set "ENGINE_MODE=%%B"
    if /I "%%A"=="tts.displayName" set "DISPLAY_NAME=%%B"
    if /I "%%A"=="tts.commandTemplate" set "COMMAND_TEMPLATE=%%B"
    if /I "%%A"=="storage.modelsDirectory" set "MODELS_DIR=%%B"
  )
)

echo [DocuPodcast] Raiz del proyecto: %CD%
echo [DocuPodcast] Verificando configuracion TTS local...
echo.
echo Archivo de configuracion: %SETTINGS_FILE%
if exist "%SETTINGS_FILE%" (
  echo Estado configuracion: encontrada
) else (
  echo Estado configuracion: no existe aun; se usaran valores iniciales.
)
echo Modo seleccionado: %ENGINE_MODE%
echo Nombre visible: %DISPLAY_NAME%
echo Carpeta de modelos: %MODELS_DIR%
echo.

if not "%DOCUPODCAST_TTS_COMMAND%"=="" (
  echo DOCUPODCAST_TTS_COMMAND esta configurado por entorno.
  echo Motor real por proceso: configurado manualmente.
) else if /I "%ENGINE_MODE%"=="xtts" (
  echo Voz IA avanzada seleccionada. La ruta normal es Configuracion ^> Motores de voz ^> Preparar automaticamente.
  echo Script tecnico opcional/rescate: scripts\30-preparar-voz-ia-avanzada-local.bat
) else if /I "%ENGINE_MODE%"=="coqui" (
  echo Voz IA avanzada seleccionada. La ruta normal es Configuracion ^> Motores de voz ^> Preparar automaticamente.
  echo Script tecnico opcional/rescate: scripts\30-preparar-voz-ia-avanzada-local.bat
) else if /I "%ENGINE_MODE%"=="piper" (
  echo Voz local simple seleccionada. La app derivara el comando desde tools\piper y models\tts\piper\voices.
  echo Ruta normal: Configuracion ^> Motores de voz ^> Verificar Voz local simple.
) else (
  echo Modo de prueba seleccionado. No genera voz real; solo sirve para diagnostico/desarrollo.
)

if not "%COMMAND_TEMPLATE%"=="" (
  echo.
  echo Plantilla de comando explicita configurada en archivo de ajustes.
)

echo.
echo Variables opcionales de entorno:
echo DOCUPODCAST_TTS_DISPLAY_NAME=%DOCUPODCAST_TTS_DISPLAY_NAME%
echo DOCUPODCAST_TTS_LANGUAGE=%DOCUPODCAST_TTS_LANGUAGE%
echo DOCUPODCAST_TTS_VOICE=%DOCUPODCAST_TTS_VOICE%
echo DOCUPODCAST_TTS_TIMEOUT_SECONDS=%DOCUPODCAST_TTS_TIMEOUT_SECONDS%
echo DOCUPODCAST_TTS_MAX_RETRIES=%DOCUPODCAST_TTS_MAX_RETRIES%

popd >nul
endlocal
