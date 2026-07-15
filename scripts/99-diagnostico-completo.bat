@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss" 2^>nul') do set "STAMP=%%I"
if not defined STAMP set "STAMP=manual"

set "REPORT_ROOT=target\diagnostico-completo"
set "REPORT_DIR=%REPORT_ROOT%\%STAMP%"
set "SUMMARY=%REPORT_DIR%\00-RESUMEN.md"
set "ANY_FAIL=0"
set "RUN_REAL_ENGINES=0"

if /I "%~1"=="--real-engines" set "RUN_REAL_ENGINES=1"
if /I "%DOCUPODCAST_RUN_REAL_ENGINES%"=="1" set "RUN_REAL_ENGINES=1"

if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"

> "%SUMMARY%" echo # Diagnostico completo DocuPodcast Studio
>> "%SUMMARY%" echo.
>> "%SUMMARY%" echo - Raiz: `%CD%`
>> "%SUMMARY%" echo - Fecha: `%DATE% %TIME%`
>> "%SUMMARY%" echo - Motores reales: `%RUN_REAL_ENGINES%`
>> "%SUMMARY%" echo.
>> "%SUMMARY%" echo ^| Paso ^| Resultado ^| Log ^|
>> "%SUMMARY%" echo ^|---^|---: ^|---^|

echo [DocuPodcast Studio] Diagnostico completo desde: %CD%
echo [DocuPodcast Studio] Reporte: %REPORT_DIR%
echo.

call :run_step "Java version" "01-java-version" "java -version"
call :run_step "Maven version" "02-maven-version" "mvn -version"
call :run_step "Verificar entorno" "03-verificar-entorno" "call scripts\00-verificar-entorno.bat"
call :run_step "Verificar toolchain" "04-verificar-toolchain" "call scripts\03-verificar-toolchain.bat"
call :run_step "Configuracion TTS" "05-tts-config" "call scripts\04-verificar-tts-config.bat"
call :run_step "Maven compile" "10-maven-compile" "mvn -DskipTests compile"
call :run_step "Maven tests" "11-maven-test" "mvn test"
call :run_step "Smoke automatico cerebro" "12-smoke-cerebro" "call scripts\18-smoke-automatico-cerebro.bat"
call :run_step "Preflight arranque motores" "20-preflight-arranque-motores" "call scripts\23-preflight-arranque-motores.bat"
call :run_step "Piper y FFmpeg locales" "21-piper-ffmpeg-local" "call scripts\24-verificar-piper-ffmpeg-local.bat"

if "%RUN_REAL_ENGINES%"=="1" (
  call :run_step "Smoke motores reales" "30-smoke-motores-reales" "call scripts\19-smoke-motores-reales.bat"
  call :run_step "Smoke modular motores producto" "31-smoke-motores-producto" "call scripts\28-smoke-motores-producto.bat"
) else (
  >> "%SUMMARY%" echo ^| Smoke motores reales ^| OMITIDO ^| Ejecuta `scripts\99-diagnostico-completo.bat --real-engines` o define `DOCUPODCAST_RUN_REAL_ENGINES=1` ^|
)

>> "%SUMMARY%" echo.
if "%ANY_FAIL%"=="0" (
  >> "%SUMMARY%" echo ## Resultado final: OK
  echo.
  echo [DocuPodcast Studio] Diagnostico completo OK.
) else (
  >> "%SUMMARY%" echo ## Resultado final: FALLARON uno o mas pasos
  echo.
  echo [DocuPodcast Studio] Diagnostico completo con FALLAS. Revisa:
  echo   %SUMMARY%
)

powershell -NoProfile -Command "Compress-Archive -Path '%REPORT_DIR%\*' -DestinationPath '%REPORT_DIR%.zip' -Force" >nul 2>nul
if exist "%REPORT_DIR%.zip" (
  echo [DocuPodcast Studio] ZIP de diagnostico: %REPORT_DIR%.zip
  >> "%SUMMARY%" echo.
  >> "%SUMMARY%" echo ZIP de diagnostico: `%REPORT_DIR%.zip`
)

popd >nul
exit /b %ANY_FAIL%

:run_step
set "STEP_NAME=%~1"
set "LOG_NAME=%~2"
set "STEP_CMD=%~3"
set "LOG_PATH=%REPORT_DIR%\%LOG_NAME%.log"

echo [DocuPodcast Studio] ==^> %STEP_NAME%
> "%LOG_PATH%" echo $ %STEP_CMD%
>> "%LOG_PATH%" echo.
cmd /d /s /c "%STEP_CMD%" >> "%LOG_PATH%" 2>&1
set "STEP_CODE=!ERRORLEVEL!"
if "!STEP_CODE!"=="0" (
  echo   OK
  >> "%SUMMARY%" echo ^| %STEP_NAME% ^| OK ^| `%LOG_NAME%.log` ^|
) else (
  echo   FALLO ^(exit !STEP_CODE!^)
  >> "%SUMMARY%" echo ^| %STEP_NAME% ^| FALLO !STEP_CODE! ^| `%LOG_NAME%.log` ^|
  set "ANY_FAIL=1"
)
exit /b 0
