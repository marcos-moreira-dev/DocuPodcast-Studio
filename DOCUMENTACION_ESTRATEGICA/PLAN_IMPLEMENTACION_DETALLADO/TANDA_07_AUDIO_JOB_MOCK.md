# Tanda 7 — Audio Job mock

## Objetivo

Implementar toda la UI/arquitectura de jobs sin motor TTS real.

## Dominio

- `AudioJob`;
- `AudioJobState`;
- `AudioSegmentStatus`;
- `AudioManifest`.

## Application

- `AudioGenerationGateway`;
- `SubmitAudioGenerationJobUseCase`;
- `CancelAudioGenerationJobUseCase`;
- `ListAudioJobsUseCase`.

## Infrastructure

- `FakeAudioGenerationGateway` que crea WAV fake o archivos txt temporales.

## UI

- Audio workspace;
- tabla de jobs;
- progreso;
- ETA fake/calculada;
- logs.

## Criterios

- Generar job por segmentos.
- Ver progreso 0–100%.
- Cancelar sin romper estado.
- Mostrar ETA después de algunos segmentos.
