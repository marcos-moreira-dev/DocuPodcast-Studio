# T88 — FFmpeg real para media y Audio del computador

## Estado

T88 cierra el primer contrato real de media del flujo Documento:

```text
Documento → seleccionar oración/bloque → Audio del computador → Elegir audio… / Extraer audio de video… → asset AUDIO_CLIP → capa HUMAN_AUDIO
```

La tanda no convierte aún las capas `HUMAN_AUDIO` en reemplazo efectivo del playback TTS por oración; eso queda para el render narrativo por `AudioUnit`. Sí elimina la brecha visual más crítica: los botones del inspector ya no solo preparan una capa sin target, sino que abren selector, importan/preparan media y asignan el `AUDIO_CLIP` real a la selección.

## Cambios principales

- Se agrega `AudioNormalizationGateway`, `AudioNormalizationProfile` y `AudioNormalizationResult`.
- Se agrega `FfmpegAudioNormalizationGateway` para normalizar audio con FFmpeg y dejar evidencia lateral.
- `UserMediaFormatPolicy` acepta audio `WAV/MP3/M4A/FLAC/OGG` y video `MP4/MOV/MKV/WEBM`.
- `ImportUserMediaAssetUseCase` puede normalizar audio no WAV a WAV asignable cuando FFmpeg está configurado.
- `FfmpegVideoAudioExtractionGateway` ahora deja sidecars de stdout/stderr y manifest de extracción.
- `DocumentAudioNarrationPanel` y el rail legado abren `FileChooser` real para `Elegir audio…` y `Extraer audio de video…`.
- `DocuPodcastShellViewModel` agrega `importUserAudioForSelectedDocumentRange(...)` y `extractVideoAudioForSelectedDocumentRange(...)` para importar media y asignar la capa `HUMAN_AUDIO` al fragmento seleccionado.

## Formatos

Audio del computador:

```text
WAV → se copia como AUDIO_CLIP.
MP3/M4A/FLAC/OGG → FFmpeg normaliza a WAV asignable si está configurado.
```

Video para extraer audio:

```text
MP4/MOV/MKV/WEBM → se conserva VIDEO_SOURCE y se crea AUDIO_CLIP derivado en WAV.
```

## Contrato de producto posterior a T88C

Whisper/STT no forma parte del producto visible DocuPodcast. T88 conserva infraestructura histórica si existe, pero el alcance de producto se limita a Coqui/XTTS, Piper y FFmpeg.

## Evidencia de FFmpeg

Para normalización/extracción se crean archivos laterales junto al WAV derivado:

```text
*-ffmpeg-stdout.log
*-ffmpeg-stderr.log
*-ffmpeg-normalization.txt
*-ffmpeg-extraction.txt
```

Estos artefactos son para diagnóstico/soporte, no para mostrarse como texto técnico en el Documento.

## Limitaciones conocidas

- La capa `HUMAN_AUDIO` queda persistida como asset real, pero el playback actual todavía consume WAVs de jobs TTS por `segmentId`. La sustitución efectiva de playback por audio humano queda para `NarrationRenderPlan` / `AudioUnit`.
- FFmpeg sigue siendo proceso síncrono con timeout; job común de proceso largo queda para la tanda de jobs transversales.
- No se descarga ni embebe FFmpeg automáticamente; se usa `tools/ffmpeg/bin/ffmpeg.exe` o la ruta configurada en Configuración.


## Criterio de producto: audio genérico

`Audio del computador` no intenta clasificar el contenido como voz humana, efecto, ambiente o música. Desde el punto de vista del programa es un clip de audio elegido por el usuario y relacionado con una selección del documento. La responsabilidad semántica del archivo queda en el usuario; el producto solo conserva procedencia, normalización, asset y asignación.
