# Tanda 16C — Hotfix scripts Windows con rutas con paréntesis

## Motivo

Después de Tanda 16B se reportó un fallo al ejecutar scripts desde PowerShell cuando el proyecto estaba dentro de una carpeta extraída con sufijo de duplicado de Windows:

```text
DocuPodcast-Studio-tanda16B-v1(1)\DocuPodcast-Studio-tanda16B-v1\scripts
```

El error observado fue:

```text
No se esperaba \DocuPodcast-Studio-tanda16B-v1\scripts\.. en este momento.
```

## Causa

Los scripts resolvían correctamente la raíz con:

```bat
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
```

pero dentro del bloque:

```bat
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde %SCRIPT_DIR%..
  exit /b 1
)
```

`cmd.exe` expandía `%SCRIPT_DIR%` antes de ejecutar el bloque. Si la ruta contenía paréntesis, el parser interpretaba parte de la ruta como cierre del bloque `if (...)`.

## Cambio aplicado

Todos los scripts públicos `.bat` ahora usan:

```bat
setlocal EnableExtensions EnableDelayedExpansion
```

Y el mensaje de error dentro del bloque usa expansión diferida:

```bat
echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
```

Se conserva el patrón root-safe:

```bat
pushd "%SCRIPT_DIR%.." >nul
```

## Scripts corregidos

```text
scripts/00-verificar-entorno.bat
scripts/01-ejecutar-app.bat
scripts/02-ejecutar-tests.bat
scripts/03-verificar-toolchain.bat
scripts/04-verificar-tts-config.bat
scripts/13-revalidacion-local-completa.bat
scripts/14-app-image-completa.bat
scripts/15-msi-completo.bat
scripts/16-release-candidate.bat
scripts/31-generar-javadoc.bat
```

## Test agregado

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/scripts/ScriptsParenthesizedPathSafetySourceTest.java
```

Este test protege que los scripts públicos:

```text
usen EnableDelayedExpansion
usen !SCRIPT_DIR! en el bloque de error
no vuelvan a expandir %SCRIPT_DIR% dentro del mensaje parentizado
```

## Validación local requerida

En Windows, incluso desde una ruta con paréntesis:

```bat
scripts-ejecutar-tests.bat
scripts-ejecutar-app.bat
```

Resultado esperado: los scripts ya no deben fallar antes de invocar Maven.

## Alcance

Esta tanda no modifica comportamiento productivo Java. Es un hotfix de scripts y guardarraíl de test fuente.

## Próxima tanda funcional

La siguiente tanda de implementación de producto sigue siendo:

```text
Tanda 17 — Guardarraíles de arquitectura y promesas visibles
```
