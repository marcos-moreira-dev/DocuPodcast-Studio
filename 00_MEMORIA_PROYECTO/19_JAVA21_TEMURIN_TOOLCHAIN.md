# Java 21 Eclipse Temurin con Maven Toolchain

El proyecto usará Java 21 con Eclipse Temurin y Maven Toolchain.

## Configuración esperada

Archivo local:

```text
%USERPROFILE%\.m2\toolchains.xml
```

Ejemplo disponible en la raíz del repositorio:

```text
toolchains.example.xml
```

## Vendor usado

Este proyecto usa la etiqueta Maven Toolchain:

```xml
<vendor>temurin</vendor>
```

El valor debe coincidir entre el `pom.xml` y el `toolchains.xml`.

## Motivo

Esto evita que Maven compile accidentalmente con otro JDK instalado en el sistema.

## Validación

```bat
scripts-verificar-toolchain.bat
mvn -q test
mvn javafx:run
```

## Regla

No diagnosticar problemas de JavaFX o compilación sin verificar primero que Maven esté usando el toolchain Java 21 Temurin.
