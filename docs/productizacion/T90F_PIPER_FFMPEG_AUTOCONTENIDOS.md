# T90F — Piper y FFmpeg autocontenidos/verificables

## Objetivo

Cerrar el contrato local de herramientas auxiliares del producto antes de rediseñar la GUI:

- Piper vive dentro de `tools/piper/` y sus voces dentro de `models/tts/piper/voices/`.
- FFmpeg vive dentro de `tools/ffmpeg/bin/`.
- No se usa `PATH`, ni instalaciones globales, ni comandos del sistema como fallback.

## Alcance

T90F agrega un preflight de herramientas locales para validar:

```text
Piper:
  tools/piper/piper.exe
  models/tts/piper/voices/*.onnx

FFmpeg:
  tools/ffmpeg/bin/ffmpeg.exe
  tools/ffmpeg/bin/ffprobe.exe (opcional para diagnóstico avanzado)
```

## Scripts

```bat
scripts\24-verificar-piper-ffmpeg-local.bat
```

El script genera:

```text
target/docupodcast-engine-setup/T90F_PIPER_FFMPEG_PREFLIGHT_REPORT.md
```

## Contrato de producto

Piper es fallback rápido de voz. FFmpeg prepara audio/video. Ambos deben ser repo-locales/app-locales para mantener DocuPodcast autocontenido y mantenible.

## No alcance

- No descarga automáticamente Piper ni FFmpeg todavía.
- No usa ejecutables globales.
- No reintroduce Whisper/STT.
