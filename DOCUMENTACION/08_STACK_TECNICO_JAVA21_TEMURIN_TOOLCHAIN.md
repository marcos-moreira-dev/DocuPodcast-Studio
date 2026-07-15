# 08 — Stack técnico: Java 21 Eclipse Temurin + Maven Toolchain

El entorno objetivo queda cerrado:

```text
Java: 21
Distribución recomendada: Eclipse Temurin
Build: Maven
Toolchain: Maven Toolchains
UI: JavaFX 21.x
```

El `pom.xml` debe usar `maven-toolchains-plugin` para pedir JDK 21. El repositorio incluye `toolchains.example.xml`.

## Configuración local

Copiar:

```text
toolchains.example.xml
```

a:

```text
%USERPROFILE%\.m2\toolchains.xml
```

Ajustar:

```xml
<jdkHome>C:\Program Files\Eclipse Adoptium\jdk-21</jdkHome>
```

## Comandos

```bat
scripts\00-verificar-entorno.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

## Decisión práctica

El POM exige versión 21. La documentación recomienda Eclipse Temurin. Para evitar errores por diferencias de string de vendor, el POM no debería depender de coincidencias frágiles de vendor salvo que se acuerde una convención exacta en `toolchains.xml`.

## No asumir

No asumir que el usuario no tiene Java correcto si Maven Toolchain está bien configurado. Maven puede compilar con Temurin 21 aunque `java -version` en PATH apunte a otra distribución.
