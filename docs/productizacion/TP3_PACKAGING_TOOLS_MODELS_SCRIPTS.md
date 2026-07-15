# TP3 — Packaging tools/models/scripts

TP3 convierte el layout definido en TP1 en un contrato de empaque verificable. La app sigue pudiendo ejecutarse en modo desarrollo, pero el producto portable/instalable ya tiene una estructura formal para herramientas, modelos, scripts y ejemplos.

## Regla central

DocuPodcast Studio no usa PATH global para motores. La ruta normal de producto es autocontenida o configurada desde Configuración:

```text
tools/
models/
scripts/tts/
src/main/resources/examples/
```

## Carpetas de producto

```text
tools/ffmpeg/bin/ffmpeg.exe
tools/ffmpeg/bin/ffprobe.exe
tools/piper/piper.exe
tools/xtts-wrapper/
tools/xtts-wrapper/.venv/Scripts/python.exe
models/tts/xtts/
models/tts/xtts/speakers/voz-por-defecto.wav
models/tts/piper/voices/
scripts/tts/
src/main/resources/examples/
```

## Motores

- **FFmpeg**: media, normalizacion de audio y video/storyboard futuro.
- **Piper**: fallback liviano de voz local.
- **Coqui/XTTS**: objetivo de calidad alta mediante wrapper local.
- **Mock integrado**: sigue disponible para diagnostico y pruebas de flujo sin motor real.

## Cambios de codigo

- `RuntimeBundleItemKind` distingue archivo/carpeta.
- `RuntimeBundleItem` describe cada item del runtime.
- `RuntimeBundleManifest` calcula faltantes y genera reporte Markdown.
- `BuildRuntimeBundleManifestUseCase` define el contrato canonico `tools/`, `models/`, `scripts/tts` y ejemplos.

## Script de verificacion

Nuevo script:

```bat
scripts\29-verificar-runtime-layout.bat
```

Genera:

```text
target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md
```

El script valida la estructura del repositorio/portable sin descargar motores ni usar instalaciones globales.

## Alcance

TP3 no redistribuye binarios de terceros. Solo deja preparada la estructura y el manifiesto. La decision legal/licencias corresponde a TP4.

## Proximo paso

TP4 debe revisar licencias y manifest de terceros para FFmpeg, Piper, Coqui/XTTS, modelos y voces.
