@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=target\runtime-layout"
set "REPORT=%REPORT_DIR%\TP3_RUNTIME_LAYOUT_REPORT.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - TP3 runtime layout
  echo.
  echo Raiz: `!CD!`
  echo.
  echo ^| Ruta ^| Estado ^|
  echo ^|---^|---: ^|
) > "%REPORT%"

set "MISSING=0"
call :checkDir "tools"
call :checkDir "models"
call :checkDir "scripts"
call :checkDir "scripts\tts"
call :checkDir "tools\ffmpeg"
call :checkDir "tools\ffmpeg\bin"
call :checkFile "tools\ffmpeg\bin\ffmpeg.exe"
call :checkFile "tools\ffmpeg\bin\ffprobe.exe"
call :checkDir "tools\piper"
call :checkDir "tools\xtts-wrapper"
call :checkDir "models\tts"
call :checkDir "models\tts\xtts"
call :checkDir "models\tts\piper\voices"
call :checkFile "tools\xtts-wrapper\synthesize_xtts.py"
call :checkFile "tools\xtts-wrapper\check_xtts_runtime.py"
call :checkFile "scripts\tts\piper-file-to-wav.ps1"
call :checkFile "scripts\tts\xtts-file-to-wav.ps1"
call :checkFile "scripts\tts\preflight-piper-ffmpeg.ps1"

if "%MISSING%"=="0" (
  echo [DocuPodcast Studio] Layout runtime TP3 OK.
  echo Reporte: %REPORT%
  popd >nul
  exit /b 0
)

echo [DocuPodcast Studio] Layout runtime TP3 incompleto. Revisa: %REPORT%
popd >nul
exit /b 1

:checkDir
if exist "%~1\" (
  echo ^| `%~1` ^| OK ^|>> "%REPORT%"
) else (
  echo ^| `%~1` ^| FALTA ^|>> "%REPORT%"
  set "MISSING=1"
)
exit /b 0

:checkFile
if exist "%~1" (
  echo ^| `%~1` ^| OK ^|>> "%REPORT%"
) else (
  echo ^| `%~1` ^| FALTA ^|>> "%REPORT%"
  set "MISSING=1"
)
exit /b 0
