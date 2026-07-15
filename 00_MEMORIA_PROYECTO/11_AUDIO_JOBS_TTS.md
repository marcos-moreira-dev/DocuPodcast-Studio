# Audio jobs y TTS

La generación de audio será lenta y debe tratarse como job persistente por segmentos.

## Flujo

```text
NarrationScript
  → AudioGenerationPlan
  → AudioJob
  → WAV por segmento
  → AudioManifest
  → podcast final WAV/MP3
```

## AudioJobStatusDto

Debe incluir:

- jobId;
- documentName;
- state;
- stage;
- completedSegments;
- totalSegments;
- failedSegments;
- progress;
- currentSegmentId;
- currentSegmentTitle;
- estimatedRemainingSeconds;
- outputDirectory;
- finalAudioPath.

## Estados

```text
QUEUED
PREPARING_SCRIPT
GENERATING_AUDIO
MERGING_AUDIO
EXPORTING
PAUSE_REQUESTED
PAUSED
CANCELLATION_REQUESTED
CANCELLED
COMPLETED
FAILED
COMPLETED_WITH_WARNINGS
```

## Cancelación

Cancelación cooperativa. No matar procesos de forma brusca si están escribiendo WAV.

## Reanudación

El job debe guardar:

- `job.json`;
- `segments-status.json`;
- audios parciales;
- `audio-manifest.json`;
- logs.

## Motor TTS

Detrás de `AudioGenerationGateway`. La UI nunca llama directamente a XTTS/Piper/Python.
