@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=dist\release-candidate"
set "REPORT=%REPORT_DIR%\PF6B_GUI_SMOKE_CHECKLIST.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - PF6B smoke real desde GUI
  echo.
  echo Fecha: %DATE% %TIME%
  echo.
  echo Este smoke se ejecuta desde la aplicacion, no desde un placeholder de consola.
  echo.
  echo 1. Abre DocuPodcast Studio.
  echo 2. Ve a Configuracion ^> Motores de voz.
  echo 3. Usa Verificar/Preparar/Importar para dejar listo un motor real.
  echo 4. Pulsa Generar checklist smoke GUI para crear el reporte asistido desde la app.
  echo 5. En Vista ^> Voces, genera una prueba de voz real.
  echo 6. En Documento, abre un archivo corto, prepara lectura y escucha desde aqui.
  echo.
  echo Criterio: no usar placeholders ni comandos manuales como flujo normal del usuario final.
) > "%REPORT%"

echo [DocuPodcast Studio] Checklist tecnico PF6B creado: %REPORT%
echo La validacion principal se completa desde Configuracion dentro de la app.
popd >nul
exit /b 0
