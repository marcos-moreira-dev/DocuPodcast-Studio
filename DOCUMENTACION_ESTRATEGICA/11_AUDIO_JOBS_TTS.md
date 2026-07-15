# Audio jobs y motor TTS

La generación de audio será por lotes y por segmentos. No se exige tiempo real.

## Inspiración

Fractal Render Studio:

```text
frames → PNG → video
```

DocuPodcast:

```text
segmentos → WAV → podcast
```

## AudioJob

```text
AudioJob
  ├── id
  ├── documentName
  ├── state
  ├── stage
  ├── completedSegments
  ├── totalSegments
  ├── failedSegments
  ├── currentSegmentId
  ├── startedAt
  ├── finishedAt
  ├── estimatedRemainingSeconds
  └── outputDirectory
```

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
COMPLETED_WITH_WARNINGS
FAILED
```

## Segment status

```text
PENDING
GENERATING
COMPLETED
FAILED
SKIPPED
CANCELLED
```

## Progreso

La UI debe mostrar:

- segmento actual;
- completados/totales;
- porcentaje;
- ETA;
- fallidos;
- logs recientes;
- botones de cancelar/reintentar/abrir carpeta.

## TTS Gateway

La UI no llama al motor TTS directamente.

```text
AudioGenerationGateway
  ├── submit(plan, statusConsumer)
  ├── cancel(jobId)
  ├── pause(jobId)
  ├── resume(jobId)
  └── retryFailed(jobId)
```

Implementaciones posibles:

- mock/fake para UI;
- Python worker empaquetado;
- Piper process;
- ONNX runtime;
- futuro motor nativo.
