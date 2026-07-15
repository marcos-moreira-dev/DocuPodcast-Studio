# PF5C — Preparación guiada completa de motores

Estado: implementada sobre PF5B/PF6A verde.

## Objetivo

Cerrar el asistente de Configuración para que el usuario final no dependa de scripts manuales cuando necesita dejar listos los componentes principales:

- Voz IA avanzada.
- Voz local simple.
- FFmpeg/FFprobe.
- Auditoría local de artefactos/checksums.

## Cambios

- `ImportPiperVoiceFolderUseCase` importa una carpeta con `.onnx` y `.onnx.json` a `models/tts/piper/voices`.
- `ImportFfmpegRuntimeFolderUseCase` busca `ffmpeg.exe` y `ffprobe.exe` en una carpeta seleccionada y los copia a `tools/ffmpeg/bin`.
- `SettingsApplicationServices` expone `importPiperVoiceFolder` e `importFfmpegRuntimeFolder`.
- Configuración → Motores de voz agrega acciones reales:
  - `Importar voz local...`
  - `Importar FFmpeg...`
- Las operaciones usan el diálogo de progreso vivo ya implementado: no quedan como acciones silenciosas ni como scripts de usuario final.

## Criterio

Los scripts siguen como rescate técnico. La ruta normal del usuario final es Configuración.
