# Tanda 9 — Reanudación avanzada con motor real y diagnóstico de proceso

## Estado

Implementada.

## Objetivo

Robustecer el gateway TTS real antes de avanzar a biblioteca de voces. La Tanda 8 permitió invocar un proceso local; esta tanda agrega diagnóstico por intento, reintentos configurables y lectura visible de `process-diagnostics.jsonl`.

## Cambios principales

- `LocalTtsProcessConfiguration` acepta `maxRetries`.
- Nuevas claves:
  - `docupodcast.tts.maxRetries`
  - `DOCUPODCAST_TTS_MAX_RETRIES`
- `LocalTtsProcessAudioGenerationGateway` reintenta segmentos fallidos hasta `maxRetries + 1` intentos.
- Cada intento registra `AudioProcessDiagnosticEvent`.
- Se persiste `jobs/JOB-*/logs/process-diagnostics.jsonl`.
- El workspace Audio muestra diagnóstico del proceso TTS real.
- La reanudación sigue conservando WAVs completados.

## Alcance

La app sigue siendo JavaFX autocontenida. No se agrega servidor ni API HTTP. El motor TTS real sigue siendo un proceso local configurable.

## Archivos nuevos

- `domain/audio/AudioProcessDiagnosticEvent.java`
- `application/audio/AudioProcessDiagnosticsRepository.java`
- `application/audio/ListAudioProcessDiagnosticsUseCase.java`
- `infrastructure/audio/AudioProcessDiagnosticsFileRepository.java`

## Tests nuevos

- `AudioProcessDiagnosticEventTest`
- `AudioProcessDiagnosticsFileRepositoryTest`
- `LocalTtsProcessGatewayDiagnosticsSourceTest`

## Validación hecha en entorno ChatGPT

- Compilación parcial `javac --release 21` de `domain`, `application`, `infrastructure` y factories no JavaFX.
- Smoke manual con worker local falso por proceso:
  - genera dos WAVs de prueba;
  - escribe `process-diagnostics.jsonl`;
  - completa job TTS real simulado.

## Pendientes relacionados

- Medir duración real de WAV con parser de cabecera WAV en vez de estimación por caracteres.
- Exportación WAV/MP3 final real.
- Voice Library.
- Configuración UI formal del motor TTS.
