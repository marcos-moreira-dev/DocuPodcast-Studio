# 28 — Tanda 5 plan: Audio mock

## Objetivo

Construir todo el pipeline visual y de estado de audio sin depender del motor real.

## Dominio

```text
AudioJob
AudioJobState
AudioSegmentStatus
AudioManifest
AudioClipReference
```

## Application

```text
AudioGenerationGateway
SubmitAudioGenerationJobUseCase
CancelAudioGenerationJobUseCase
ListAudioJobsUseCase
```

## Infrastructure mock

```text
MockAudioGenerationGateway
MockWavWriter
```

## Presentation

```text
AudioWorkspaceView
AudioJobQueueView
AudioJobRow
GenerationLogView
ProgressPanel
```

## Criterio

Generar archivos WAV falsos por segmento, progreso real, ETA simulada, cancelación segura.
