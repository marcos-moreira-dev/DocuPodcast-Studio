# Tanda 32 — STT / Audio a texto con Whisper.cpp

## Objetivo

Convertir la acción “Audio a texto” en una capacidad real y honesta, basada en un motor local configurable por proceso externo.

## Cambios

- Se agrega la familia `application.stt` con gateway, request, result, descriptor y use cases.
- Se agrega `WhisperCppSpeechToTextGateway` para ejecutar `whisper.cpp` / `whisper-cli` por subprocess.
- Se agrega `JavaSoundAudioNormalizer` para normalizar audio soportado a WAV PCM 16 kHz mono.
- `DocuPodcastShellView` expone `Guion → Transcribir audio a texto…`.
- `DocuPodcastShellViewModel.transcribeAudioFileToSelectedSegment(Path)` actualiza el segmento seleccionado con el transcript puro.
- El transcript queda guardado bajo `stt/transcripts/` y registrado como asset `OTHER`.

## Configuración local

```text
DOCUPODCAST_WHISPER_CPP
DOCUPODCAST_WHISPER_MODEL
DOCUPODCAST_STT_LANGUAGE
DOCUPODCAST_STT_TIMEOUT_SECONDS
```

## Alcance

Esta tanda implementa STT puro. No llama LLM, no resume, no corrige con IA y no modifica varios segmentos automáticamente.
