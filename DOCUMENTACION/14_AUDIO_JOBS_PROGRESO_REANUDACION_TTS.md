# 14 — Audio jobs, progreso, reanudación y TTS

El audio se genera por segmentos. No se genera documento completo en un solo llamado.

## Flujo

```text
NarrationScriptDocument
  → AudioGenerationPlan
  → AudioJob
  → WAV por segmento
  → audio-manifest.json
  → podcast.wav / podcast.mp3
```

## AudioJobStatusDto

Debe incluir:

```text
jobId
documentName
state
stage
completedSegments
totalSegments
failedSegments
progress
currentSegmentId
currentSegmentTitle
estimatedRemainingSeconds
message
outputDirectory
finalAudioPath
```

## Estados

```text
QUEUED
PREPARING
GENERATING_AUDIO
MERGING_AUDIO
EXPORTING
PAUSED
CANCELLED
COMPLETED
FAILED
COMPLETED_WITH_WARNINGS
```

## Reanudación

Cada job debe guardar:

```text
job.json
segments-status.json
audio-manifest.json
generation-log.jsonl
```

Cancelar no borra segmentos generados. Pausar termina el segmento actual y no inicia el siguiente.

## TTS

La UI habla con `AudioGenerationGateway`. Implementaciones posibles:

```text
MockAudioGenerationGateway
PiperProcessGateway
XttsPythonWorkerGateway
OnnxRuntimeGateway futuro
```

No se invoca TTS directamente desde JavaFX.
