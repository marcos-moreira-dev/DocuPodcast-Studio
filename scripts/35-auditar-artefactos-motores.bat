@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=target\legal"
set "REPORT=%REPORT_DIR%\ENGINE_ARTIFACTS_AUDIT.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - PF5A auditoria local de artefactos de motores
  echo.
  echo Raiz: `!CD!`
  echo Fecha: `%DATE% %TIME%`
  echo.
  echo Esta auditoria no descarga ni instala. Solo revisa los artefactos que ya existen y calcula SHA-256 cuando hay archivo concreto.
  echo.
  echo ^| Artefacto ^| Ruta esperada ^| Estado ^| SHA-256 ^|
  echo ^|---^|---^|---: ^|---^|
) > "%REPORT%"

call :auditFile "FFmpeg executable" "tools\ffmpeg\bin\ffmpeg.exe"
call :auditFile "FFprobe executable" "tools\ffmpeg\bin\ffprobe.exe"
call :auditFile "Python local Voz IA" "tools\xtts-wrapper\.venv\Scripts\python.exe"
call :auditFile "Wrapper Voz IA" "tools\xtts-wrapper\synthesize_xtts.py"
call :auditFile "Modelo config" "models\tts\xtts\config.json"
call :auditFile "Modelo weights" "models\tts\xtts\model.pth"
call :auditFile "Modelo vocab" "models\tts\xtts\vocab.json"
call :auditFile "Modelo speakers" "models\tts\xtts\speakers_xtts.pth"
call :auditFile "Modelo dvae" "models\tts\xtts\dvae.pth"
call :auditFile "Modelo mel stats" "models\tts\xtts\mel_stats.pth"
call :auditFile "Voz neutral" "models\tts\xtts\speakers\voz-por-defecto.wav"
call :auditFile "Piper executable" "tools\piper\piper.exe"

(
  echo.
  echo ## Siguiente accion
  echo - Si faltan runtime/modelos, usa Configuracion ^> Motores de voz desde la app.
  echo - Para RC final, fija licencias y checksums en dist\legal antes de distribuir.
) >> "%REPORT%"

echo [DocuPodcast Studio] Auditoria de artefactos de motores generada.
echo Reporte: %REPORT%
popd >nul
exit /b 0

:auditFile
set "LABEL=%~1"
set "FILE=%~2"
if exist "%FILE%" (
  set "HASH=PENDIENTE"
  for /f "skip=1 tokens=*" %%H in ('certutil -hashfile "%FILE%" SHA256 2^>nul ^| findstr /r /v "CertUtil"') do (
    if not "%%H"=="" set "HASH=%%H"
  )
  echo ^| !LABEL! ^| `!FILE!` ^| OK ^| !HASH! ^|>> "%REPORT%"
) else (
  echo ^| !LABEL! ^| `!FILE!` ^| FALTA ^| - ^|>> "%REPORT%"
)
exit /b 0
