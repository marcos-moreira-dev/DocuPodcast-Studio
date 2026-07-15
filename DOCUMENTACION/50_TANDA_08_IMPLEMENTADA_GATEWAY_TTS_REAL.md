# Tanda 8 — Gateway TTS real

## Estado

Implementada como gateway real por proceso local configurable.

## Qué se agregó

- `AudioEngineDescriptor` para diferenciar mock vs TTS real.
- `GetAudioEngineDescriptorUseCase` para diagnóstico visible del motor activo.
- `LocalTtsProcessConfiguration` para leer comando desde propiedades JVM o variables de entorno.
- `LocalTtsProcessAudioGenerationGateway` como implementación real de `AudioGenerationGateway`.
- Selección automática de gateway en `InfrastructureServicesFactory`:
  - si existe `docupodcast.tts.command` o `DOCUPODCAST_TTS_COMMAND`, usa TTS real por proceso;
  - si no existe, conserva `MockAudioGenerationGateway`.
- El workspace Audio muestra el motor activo.
- La toolbar deja de decir “mock” y pasa a “Generar audio”.

## Contrato del proceso TTS

El comando debe recibir un TXT por segmento y devolver un WAV por segmento. Placeholders:

- `{textFile}` / `{input}`
- `{outputFile}` / `{output}`
- `{segmentId}`
- `{language}`
- `{voice}` / `{voiceProfileId}`

Ejemplo:

```bat
set DOCUPODCAST_TTS_COMMAND=runtime\tts\tts_worker.exe --text-file {textFile} --output-file {outputFile} --language {language} --voice {voice}
```

## Alcance real

Esta tanda no empaqueta XTTS, Piper ni modelos. La razón es deliberada: los modelos pesan mucho y la app debe mantener el motor TTS detrás de un gateway intercambiable.

## Por qué no API

El gateway no abre FastAPI ni servidor. JavaFX sigue siendo la app visible. El motor real es un proceso local interno, igual que una app desktop que invoca FFmpeg.

## Resultado

La arquitectura ya puede generar WAVs reales por segmento si el usuario configura un worker compatible. Sin configuración, la app sigue funcionando con mock.
