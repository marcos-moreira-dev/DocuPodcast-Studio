@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Prueba local de Coqui/XTTS
echo.
set "PYTHON=tools\xtts-wrapper\.venv\Scripts\python.exe"
set "WRAPPER=tools\xtts-wrapper\synthesize_xtts.py"
set "MODEL_DIR=models\tts\xtts"
set "SPEAKER=models\tts\xtts\speakers\voz-por-defecto.wav"
set "INPUT_DIR=target\docupodcast-engine-setup\input"
set "OUTPUT_DIR=target\docupodcast-engine-setup\output"
set "LOG_DIR=target\docupodcast-engine-setup\logs"
set "TEXT_FILE=%INPUT_DIR%\coqui-smoke.txt"
set "OUTPUT_WAV=%OUTPUT_DIR%\coqui-xtts-onboarding.wav"

if not exist "%PYTHON%" (
  echo Falta el Python local de Coqui/XTTS: %PYTHON%
  echo Ejecuta primero: scripts\20-preparar-python-portable-coqui.bat
  popd >nul
  exit /b 1
)

if not exist "%INPUT_DIR%" mkdir "%INPUT_DIR%"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
> "%TEXT_FILE%" echo Esta es una prueba corta de voz para DocuPodcast Studio.

powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\tts\xtts-file-to-wav.ps1" ^
  -Python "%CD%\%PYTHON%" ^
  -Wrapper "%CD%\%WRAPPER%" ^
  -ModelDir "%CD%\%MODEL_DIR%" ^
  -SpeakerWav "%CD%\%SPEAKER%" ^
  -Text "%CD%\%TEXT_FILE%" ^
  -Output "%CD%\%OUTPUT_WAV%" ^
  -Language "es" ^
  1> "%LOG_DIR%\coqui-onboarding-stdout.log" ^
  2> "%LOG_DIR%\coqui-onboarding-stderr.log"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] Fallo la prueba de Coqui/XTTS.
  echo Revisa:
  echo   %LOG_DIR%\coqui-onboarding-stderr.log
  popd >nul
  exit /b %EXIT_CODE%
)

if not exist "%OUTPUT_WAV%" (
  echo [DocuPodcast Studio] Coqui/XTTS no genero el WAV esperado: %OUTPUT_WAV%
  popd >nul
  exit /b 1
)

echo.
echo [DocuPodcast Studio] Coqui/XTTS genero WAV correctamente:
echo   %OUTPUT_WAV%
popd >nul
exit /b 0
