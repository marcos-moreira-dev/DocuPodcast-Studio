# Tanda 88 — FFmpeg/media real

T88 conecta el inspector de Documento con media real: `Elegir audio…` y `Extraer audio de video…` abren selector, importan/preparan archivos dentro de la carpeta del proyecto y asignan un `AUDIO_CLIP` real como capa `HUMAN_AUDIO` al fragmento seleccionado.

## Alcance

- Audio aceptado: WAV/MP3/M4A/FLAC/OGG.
- Video aceptado: MP4/MOV/MKV/WEBM.
- WAV se copia como audio asignable.
- Audio no WAV se normaliza con FFmpeg a WAV asignable.
- Video se conserva como `VIDEO_SOURCE` y se deriva un WAV `AUDIO_CLIP`.
- STT puede pedir WAV 16 kHz mono a través del mismo contrato de normalización FFmpeg.

## Limitación

La capa `HUMAN_AUDIO` queda real y persistida, pero el playback sigue basado en jobs TTS por segmento hasta introducir `NarrationRenderPlan` / `AudioUnit`.
