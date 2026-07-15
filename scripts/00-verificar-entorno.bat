@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)
set "PROJECT_ROOT=%CD%"

echo [DocuPodcast Studio] Raiz del proyecto: %PROJECT_ROOT%
echo Verificando entorno de DocuPodcast Studio...
echo.

echo Java disponible en PATH:
call java -version
if errorlevel 1 (
  echo ERROR: Java no esta disponible en PATH.
  popd >nul
  exit /b 1
)

echo.
echo Maven disponible:
call mvn -version
if errorlevel 1 (
  echo ERROR: Maven no esta disponible.
  popd >nul
  exit /b 1
)

echo.
echo Verificando Maven Toolchain Java 21 Temurin...
if not exist "%USERPROFILE%\.m2\toolchains.xml" (
  echo ADVERTENCIA: No existe %USERPROFILE%\.m2\toolchains.xml
  echo Copia toolchains.example.xml o .mvn\toolchains.xml.example a esa ruta y ajusta jdkHome.
) else (
  echo toolchains.xml encontrado: %USERPROFILE%\.m2\toolchains.xml
)

echo.
echo Nota: Maven puede estar ejecutandose con otro JDK en mvn -version.
echo El build formal debe seleccionar Temurin 21 mediante Maven Toolchains.
echo.
echo Ejecutando validacion Maven rapida desde la raiz del proyecto...
call mvn -q -DskipTests validate
if errorlevel 1 (
  echo ERROR: Maven validate fallo. Revisa toolchains.xml, Java 21 Temurin y dependencias.
  popd >nul
  exit /b 1
)

echo.
echo Entorno basico OK.
popd >nul
endlocal
