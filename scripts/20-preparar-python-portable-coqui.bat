@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Preparando Python local para Coqui/XTTS
echo.
echo Este proceso instala un runtime Python dentro del repositorio:
echo   tools\python\python-nuget-3.10.11\tools\python.exe
echo y crea el entorno aislado:
echo   tools\xtts-wrapper\.venv
echo.
echo No modifica PATH global ni instala dependencias dentro de proyectos .docupodcast.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\tts\setup-xtts-portable-python.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] No se pudo preparar Coqui/XTTS.
  echo Revisa:
  echo   target\docupodcast-engine-setup\T90B_COQUI_PYTHON_SETUP_REPORT.md
  popd >nul
  exit /b %EXIT_CODE%
)

echo.
echo [DocuPodcast Studio] Python local para Coqui/XTTS preparado.
echo Reporte:
echo   target\docupodcast-engine-setup\T90B_COQUI_PYTHON_SETUP_REPORT.md
popd >nul
exit /b 0
