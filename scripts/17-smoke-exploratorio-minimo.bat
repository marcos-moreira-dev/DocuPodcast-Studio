@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Smoke exploratorio minimo T58C
echo.
echo 1. Confirma primero que la base esta verde:
echo    scripts\02-ejecutar-tests.bat
echo.
echo 2. Documento sugerido:
echo    samples\smoke\documento-simple-t58c.docx
echo.
echo 3. Checklist:
echo    docs\testeo\checklists\58C_smoke_exploratorio_minimo.md
echo.
echo 4. Reporte manual:
echo    docs\testeo\reportes\REPORTE_SMOKE_EXPLORATORIO_TANDA_58C.md
echo.
echo Este script no automatiza la UI: solo centraliza la ruta del smoke.
echo.
choice /C SN /M "Abrir la app ahora con scripts\01-ejecutar-app.bat?"
if errorlevel 2 goto :end
call scripts\01-ejecutar-app.bat
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo [DocuPodcast Studio] La app termino con error durante el smoke.
  popd >nul
  exit /b %EXIT_CODE%
)

:end
echo.
echo [DocuPodcast Studio] Completa el checklist y el reporte manual T58C.
popd >nul
exit /b 0
