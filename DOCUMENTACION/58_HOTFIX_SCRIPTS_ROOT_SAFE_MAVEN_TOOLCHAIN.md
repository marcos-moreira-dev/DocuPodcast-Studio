# Hotfix scripts — ejecución desde cualquier carpeta

Esta corrección resuelve un problema detectado al ejecutar scripts desde `scripts\`.

## Problema observado

Al ejecutar:

```bat
cd scripts
.\02-ejecutar-tests.bat
```

Maven fallaba con:

```text
The goal you specified requires a project to execute but there is no POM in this directory (...\scripts)
```

Además, algunos scripts invocaban `mvn` sin `call`, lo cual en Windows puede transferir el control al batch de Maven y evitar que el script continúe.

## Corrección aplicada

Todos los scripts principales ahora:

1. Resuelven su ubicación con `%~dp0`.
2. Hacen `pushd "%SCRIPT_DIR%.."` para moverse a la raíz del repositorio.
3. Invocan Maven con `call mvn ...`.
4. Devuelven el código de salida correcto.

Scripts corregidos:

```text
scripts/00-verificar-entorno.bat
scripts/01-ejecutar-app.bat
scripts/02-ejecutar-tests.bat
scripts/03-verificar-toolchain.bat
scripts/04-verificar-tts-config.bat
```

## Maven runtime vs Toolchain

Es normal que `mvn -version` pueda mostrar otro JDK si Maven fue iniciado con otro `JAVA_HOME`.
El proyecto compila formalmente con Java 21 Temurin mediante Maven Toolchain, siempre que exista:

```text
%USERPROFILE%\.m2\toolchains.xml
```

con:

```xml
<version>21</version>
<vendor>temurin</vendor>
```

El `pom.xml` usa toolchain para compilación y tests.

## Test agregado

Se agregó `ScriptsRootSafeSourceTest` para evitar regresiones:

- los scripts Maven deben resolver raíz;
- los scripts Maven deben usar `call mvn`.

## Validación esperada

Ahora estos comandos pueden ejecutarse desde la raíz o desde `scripts\`:

```bat
.\00-verificar-entorno.bat
.\03-verificar-toolchain.bat
.\02-ejecutar-tests.bat
.\01-ejecutar-app.bat
```
