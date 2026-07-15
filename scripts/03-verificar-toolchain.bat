@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

echo [DocuPodcast Studio] Raiz del proyecto: %CD%
echo [DocuPodcast Studio] Validando Maven Toolchain Java 21 Temurin...
echo.

if not exist "%USERPROFILE%\.m2\toolchains.xml" (
  echo ADVERTENCIA: No existe %USERPROFILE%\.m2\toolchains.xml
  echo Copia toolchains.example.xml o .mvn\toolchains.xml.example a esa ruta y ajusta jdkHome.
  echo.
)

echo Maven runtime actual:
call mvn -version
if errorlevel 1 (
  echo ERROR: Maven no esta disponible.
  popd >nul
  exit /b 1
)

echo.
echo Toolchains descubiertos por Maven:
call mvn org.apache.maven.plugins:maven-toolchains-plugin:3.2.0:display-discovered-jdk-toolchains
if errorlevel 1 (
  echo ERROR: No se pudieron listar toolchains.
  popd >nul
  exit /b 1
)

echo.
echo Validando que el POM puede seleccionar Java 21 Temurin...
call mvn -q -DskipTests validate
if errorlevel 1 (
  echo ERROR: El POM no pudo validar con el toolchain Java 21 Temurin.
  echo Revisa %USERPROFILE%\.m2\toolchains.xml y que vendor/version coincidan: temurin / 21.
  popd >nul
  exit /b 1
)

echo.
echo Toolchain OK para validacion Maven.
popd >nul
endlocal
