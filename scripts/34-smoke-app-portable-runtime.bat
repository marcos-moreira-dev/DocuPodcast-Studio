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
set "REPORT_DIR=dist\release-candidate"
set "REPORT=%REPORT_DIR%\PF4B_PORTABLE_RUNTIME_SMOKE_REPORT.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - PF4B smoke app portable runtime
  echo.
  echo Raiz: `!CD!`
  echo Fecha: `%DATE% %TIME%`
  echo.
  echo ^| Verificacion ^| Estado ^|
  echo ^|---^|---: ^|
) > "%REPORT%"

set "FAILED=0"
call :checkDir "%APP_IMAGE%" "app-image generado"
call :checkFile "%APP_IMAGE%\%APP_NAME%.exe" "ejecutable app-image"
call :checkFile "%APP_IMAGE%\run-docupodcast-studio.bat" "launcher app-image"
call :checkContains "%APP_IMAGE%\run-docupodcast-studio.bat" "DOCUPODCAST_APP_ROOT=%%~dp0" "launcher define DOCUPODCAST_APP_ROOT"
call :checkDir "%PORTABLE_DIR%" "carpeta portable preparada"
call :checkFile "%PORTABLE_DIR%\%APP_NAME%.exe" "ejecutable portable"
call :checkFile "%PORTABLE_DIR%\run-docupodcast-studio.bat" "launcher portable"
call :checkContains "%PORTABLE_DIR%\run-docupodcast-studio.bat" "DOCUPODCAST_APP_ROOT=%%~dp0" "launcher portable define DOCUPODCAST_APP_ROOT"
call :checkFile "%APP_IMAGE%\branding\docupodcast-icon.ico" "branding app-image ico"
call :checkFile "%APP_IMAGE%\branding\docupodcast-icon.png" "branding app-image png"
call :checkFile "%PORTABLE_DIR%\branding\docupodcast-icon.ico" "branding portable ico"
call :checkFile "%PORTABLE_DIR%\branding\docupodcast-icon.png" "branding portable png"
call :checkDir "%PORTABLE_DIR%\tools" "tools viajan con portable si existian"
call :checkDir "%PORTABLE_DIR%\models" "models viajan con portable si existian"
call :checkDir "%PORTABLE_DIR%\scripts\tts" "scripts tts viajan con portable"
call :checkFile "dist\portable\PORTABLE_MANIFEST.txt" "manifest portable"
call :checkContains "dist\portable\PORTABLE_MANIFEST.txt" "Runtime root esperado al ejecutar portable" "manifest declara runtime root"
call :checkContains "dist\portable\PORTABLE_MANIFEST.txt" "branding\docupodcast-icon.ico" "manifest declara branding portable"

if "%FAILED%"=="0" (
  echo [DocuPodcast Studio] PF4B smoke app portable runtime OK.
  echo Reporte: %REPORT%
  popd >nul
  exit /b 0
)

echo [DocuPodcast Studio] PF4B smoke app portable runtime FALLO. Revisa: %REPORT%
popd >nul
exit /b 1

:checkDir
if exist "%~1\" (
  echo ^| %~2 ^| OK ^|>> "%REPORT%"
) else (
  echo ^| %~2 ^| FALTA `%~1` ^|>> "%REPORT%"
  set "FAILED=1"
)
exit /b 0

:checkFile
if exist "%~1" (
  echo ^| %~2 ^| OK ^|>> "%REPORT%"
) else (
  echo ^| %~2 ^| FALTA `%~1` ^|>> "%REPORT%"
  set "FAILED=1"
)
exit /b 0

:checkContains
if not exist "%~1" (
  echo ^| %~3 ^| FALTA_ARCHIVO `%~1` ^|>> "%REPORT%"
  set "FAILED=1"
  exit /b 0
)
findstr /c:"%~2" "%~1" >nul 2>nul
if errorlevel 1 (
  echo ^| %~3 ^| NO_ENCONTRADO `%~2` ^|>> "%REPORT%"
  set "FAILED=1"
) else (
  echo ^| %~3 ^| OK ^|>> "%REPORT%"
)
exit /b 0
