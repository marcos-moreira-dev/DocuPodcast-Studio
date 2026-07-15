@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "PYTHON=tools\xtts-wrapper\.venv\Scripts\python.exe"
set "CHECKER=tools\xtts-wrapper\check_xtts_runtime.py"
set "MODEL_DIR=models\tts\xtts"
set "SPEAKER=models\tts\xtts\speakers\voz-por-defecto.wav"

if not exist "%PYTHON%" (
  echo Falta el Python local de Coqui/XTTS: %PYTHON%
  echo Ejecuta primero: scripts\20-preparar-python-portable-coqui.bat
  popd >nul
  exit /b 1
)

"%PYTHON%" "%CHECKER%" --model-dir "%CD%\%MODEL_DIR%" --speaker-wav "%CD%\%SPEAKER%"
set "EXIT_CODE=%ERRORLEVEL%"
popd >nul
exit /b %EXIT_CODE%
