@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=target\docupodcast-engine-setup"
set "REPORT=%REPORT_DIR%\PF2A_VOZ_IA_AVANZADA_LOCAL_REPORT.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

> "%REPORT%" echo # PF2A - Preparacion guiada de Voz IA avanzada local
>> "%REPORT%" echo.
>> "%REPORT%" echo - Raiz: `%CD%`
>> "%REPORT%" echo - Politica: runtime local bajo `tools/`, modelos bajo `models/`, sin Python global ni PATH.
>> "%REPORT%" echo - Repositorio TTS local opcional: `%~1`
>> "%REPORT%" echo.

echo [DocuPodcast Studio] Preparando Voz IA avanzada local
echo.
echo Este asistente intenta dejar listo el runtime Python local y luego verifica modelo + voz.
echo Si aun no copiaste el modelo XTTS en models\tts\xtts, el runtime puede quedar preparado
echo pero la verificacion final pedira los archivos del modelo.
echo.

if "%~1"=="" (
  call scripts\20-preparar-python-portable-coqui.bat
) else (
  call scripts\20-preparar-python-portable-coqui.bat -LocalTtsRepo "%~1"
)
set "SETUP_CODE=%ERRORLEVEL%"
if not "%SETUP_CODE%"=="0" (
  >> "%REPORT%" echo ^| Preparar runtime Python ^| FALLO %SETUP_CODE% ^|
  echo.
  echo [DocuPodcast Studio] Fallo la preparacion del runtime. Revisa:
  echo   target\docupodcast-engine-setup\T90B_COQUI_PYTHON_SETUP_REPORT.md
  popd >nul
  exit /b %SETUP_CODE%
)
>> "%REPORT%" echo ^| Preparar runtime Python ^| OK ^|

call scripts\22-verificar-coqui-xtts-local.bat
set "VERIFY_CODE=%ERRORLEVEL%"
if not "%VERIFY_CODE%"=="0" (
  >> "%REPORT%" echo ^| Verificar modelo y muestra ^| REQUIERE_PREPARACION %VERIFY_CODE% ^|
  >> "%REPORT%" echo.
  >> "%REPORT%" echo ## Accion pendiente
  >> "%REPORT%" echo Copia el modelo local en `models\tts\xtts` con `config.json`, `model.pth` y `vocab.json`, y conserva la muestra `models\tts\xtts\speakers\voz-por-defecto.wav` o una muestra propia.
  echo.
  echo [DocuPodcast Studio] Runtime Python preparado, pero falta completar modelo/muestra.
  echo Revisa: %REPORT%
  popd >nul
  exit /b %VERIFY_CODE%
)
>> "%REPORT%" echo ^| Verificar modelo y muestra ^| OK ^|

call scripts\21-probar-coqui-xtts.bat
set "SMOKE_CODE=%ERRORLEVEL%"
if not "%SMOKE_CODE%"=="0" (
  >> "%REPORT%" echo ^| Smoke WAV corto ^| FALLO %SMOKE_CODE% ^|
  echo.
  echo [DocuPodcast Studio] El runtime parece completo pero la prueba WAV fallo.
  echo Revisa: target\docupodcast-engine-setup\logs
  popd >nul
  exit /b %SMOKE_CODE%
)
>> "%REPORT%" echo ^| Smoke WAV corto ^| OK ^|

echo.
echo [DocuPodcast Studio] Voz IA avanzada local lista para pruebas reales.
echo Reporte: %REPORT%
popd >nul
exit /b 0
