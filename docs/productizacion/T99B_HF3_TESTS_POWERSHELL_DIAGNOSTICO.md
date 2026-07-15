# T99B-HF3 — Tests de navegación, PowerShell 5.1 y diagnóstico unificado

## Objetivo

Esta tanda corrige los fallos detectados por `scripts\99-diagnostico-completo.bat` en Windows después de T99B-HF2.

El diagnóstico local confirmó:

- Compilación Maven: OK.
- Smoke automático del cerebro: OK.
- Tests Maven: 2 fallos en `WorkspaceNavigationCoordinatorTest`.
- Preflight de motores y Piper/FFmpeg: fallos de parser en PowerShell 5.1 por caracteres UTF-8 sin BOM en scripts `.ps1`.

## Correcciones

### 1. Tests de navegación alineados con T99A/T99B

`WorkspaceNavigationCoordinator` normaliza superficies legacy hacia Documento:

- `AUDIO_JOBS` → `DOCUMENT_READER`.
- `STORYBOARD` → `DOCUMENT_READER`.

Los tests estaban esperando el valor legacy original. Se actualizaron para verificar el contrato vigente: Audio Jobs y Storyboard no deben volver como workspaces visibles/restaurables; viven como overlay o rail dentro de Documento.

Archivo actualizado:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/WorkspaceNavigationCoordinatorTest.java
```

### 2. Scripts PowerShell ASCII-safe

Windows PowerShell 5.1 puede interpretar archivos UTF-8 sin BOM como ANSI. Los scripts tenían un guion largo `—` en strings Markdown, que en Windows se parseaba como comillas corruptas y rompía el archivo antes de generar reporte.

Se normalizaron los scripts `.ps1` a texto ASCII-safe para evitar parser errors en PowerShell 5.1.

Scripts afectados:

```text
scripts/tts/preflight-startup-engines.ps1
scripts/tts/preflight-piper-ffmpeg.ps1
scripts/tts/setup-xtts-portable-python.ps1
scripts/tts/xtts-file-to-wav.ps1
scripts/stt/whisper-file-to-text.ps1
```

### 3. Guardarraíl contra regresión PowerShell

Se agregó un test fuente para exigir que los `.ps1` bajo `scripts/` se mantengan ASCII-safe. Esto evita reintroducir caracteres que PowerShell 5.1 pueda parsear mal en máquinas Windows sin BOM.

Archivo actualizado:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/scripts/ScriptsRootSafeSourceTest.java
```

## Qué no cambia

Esta tanda no implementa T99C, no rediseña GUI, no modifica comandos productivos y no instala motores reales. Solo corrige base roja/local y robustez de scripts.

Los preflights de motores pueden seguir reportando `REQUIERE_PREPARACION` si faltan artefactos locales como Python embebido, modelos, Piper o FFmpeg. Eso ya no debería ser error de parser; debe quedar registrado como estado de preparación del entorno.

## Validación realizada en entorno ChatGPT

No se ejecutó Maven porque `mvn` no está instalado en este entorno.

Se validó:

- Compilación focal con `javac --release 21` de los tests modificados usando stubs mínimos de JUnit.
- Ejecución reflexiva de 5 métodos `@Test` modificados.
- Verificación de que no quedan bytes no ASCII en scripts `.ps1` bajo `scripts/`.
- ZIP íntegro.

Prueba local recomendada:

```bat
scripts\99-diagnostico-completo.bat
```
