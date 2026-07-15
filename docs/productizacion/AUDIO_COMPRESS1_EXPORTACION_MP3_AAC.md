# AUDIO-COMPRESS1 — Exportación final MP3/AAC

## Objetivo

Permitir que la salida final de audio se exporte como WAV, MP3 o AAC sin cambiar la arquitectura interna de generación: los segmentos, cachés y jobs de trabajo siguen siendo WAV.

## Contrato de producto

- El usuario usa **Exportar audio**.
- El selector permite `.wav`, `.mp3` y `.aac`.
- WAV se exporta como antes: copia audio final o concatena segmentos PCM compatibles.
- MP3/AAC se generan a partir de un WAV de trabajo y se comprimen con el componente local de video/audio.
- Si el componente local no está preparado, la app debe explicar que se puede preparar Video local en Configuración o exportar WAV.

## Decisiones técnicas

- Nuevo `AudioExportFormat` para detectar formato final por extensión.
- Nuevo `ExportPodcastAudioUseCase` como fachada de salida final.
- `ExportPodcastWavUseCase` se conserva como base de trabajo y para exportación WAV.
- MP3 usa `libmp3lame` con bitrate `192k`.
- AAC usa encoder `aac` con bitrate `192k`.
- El reporte final queda como `*.audio-export-report.md`.

## Fuera de alcance

- No se cambian chunks internos a MP3/AAC.
- No se modifica reproducción del lector.
- No se implementa todavía overlay de progreso para exportación larga.
- No se revisa Voz IA avanzada ni su descarga.
