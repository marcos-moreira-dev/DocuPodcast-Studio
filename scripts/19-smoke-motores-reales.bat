@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Smoke real de motores T90
echo.
echo Este smoke es opt-in: requiere motores/modelos locales reales.
echo Alcance: motor avanzado, voz local simple y FFmpeg. No valida transcripcion de audio.
echo.
echo Rutas por defecto:
echo   Coqui script: scripts\tts\xtts-file-to-wav.ps1
echo   Coqui wrapper: tools\xtts-wrapper\synthesize_xtts.py
echo   Coqui modelo: models\tts\xtts
echo   Coqui voz: models\tts\xtts\speakers\voz-por-defecto.wav
echo   Piper exe: tools\piper\piper.exe
echo   Piper voces: models\tts\piper\voices
echo   FFmpeg: tools\ffmpeg\bin\ffmpeg.exe
echo.
echo Puedes sobrescribir rutas de modelos/Piper/FFmpeg con propiedades; Coqui siempre usa Python local.
echo.
set "REQUIRED_ENGINES=%~1"
if "%REQUIRED_ENGINES%"=="" set "REQUIRED_ENGINES=coqui,piper,ffmpeg"
echo Motores obligatorios de esta corrida: %REQUIRED_ENGINES%
echo.
echo Si falla Coqui por falta de Python local repo-local, ejecuta primero:
echo   scripts\20-preparar-python-portable-coqui.bat
echo   scripts\21-probar-coqui-xtts.bat
echo.

call mvn -Dtest=RealEnginesSmokeScenarioTest -Ddocupodcast.realEnginesSmoke.enabled=true -Ddocupodcast.realEnginesSmoke.required=%REQUIRED_ENGINES% test
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] Smoke real de motores FALLO.
  echo Revisa:
  echo   target\docupodcast-real-engines-smoke\T90_REAL_ENGINES_SMOKE_REPORT.md
  echo   target\docupodcast-real-engines-smoke\logs
  popd >nul
  exit /b %EXIT_CODE%
)

echo.
echo [DocuPodcast Studio] Smoke real de motores OK.
echo Evidencia:
echo   target\docupodcast-real-engines-smoke\T90_REAL_ENGINES_SMOKE_REPORT.md
echo   target\docupodcast-real-engines-smoke\input
echo   target\docupodcast-real-engines-smoke\output
echo   target\docupodcast-real-engines-smoke\logs
popd >nul
exit /b 0
