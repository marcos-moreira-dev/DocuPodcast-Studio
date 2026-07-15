# Entorno Java 21 Eclipse Temurin + Maven Toolchain

Decisión cerrada: DocuPodcast Studio se compila con **Java 21 Eclipse Temurin** usando **Maven Toolchain**.

## Por qué Toolchain

Maven Toolchain evita depender del `JAVA_HOME` activo en la consola. El build puede seleccionar explícitamente un JDK 21 aunque el sistema tenga otros JDKs instalados.

## Configuración objetivo

- JDK: Java 21.
- Distribución: Eclipse Temurin.
- Maven: con `maven-toolchains-plugin`.
- JavaFX: 21.x.

## Archivo local esperado

Maven busca el archivo real en:

```text
%USERPROFILE%\.m2\toolchains.xml
```

El repositorio incluye un ejemplo en:

```text
.mvn/toolchains.xml.example
```

## Ejemplo

```xml
<toolchain>
    <type>jdk</type>
    <provides>
        <version>21</version>
        <vendor>temurin</vendor>
    </provides>
    <configuration>
        <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-21</jdkHome>
    </configuration>
</toolchain>
```

## Scripts

- `scripts/00-verificar-entorno.bat`: verifica Java, Maven y `toolchains.xml`.
- `scripts/02-ejecutar-tests.bat`: ejecuta tests.
- `scripts/01-ejecutar-app.bat`: abre la app con JavaFX Maven Plugin.

## Regla para futuras sesiones

No asumir que el proyecto tiene problema de JDK si falla una build sin revisar primero:

1. `mvn -version`.
2. `%USERPROFILE%\.m2\toolchains.xml`.
3. ruta real de Temurin 21.
4. configuración del `maven-toolchains-plugin`.
