# Memoria — Tanda 7 Audio job mock

Tanda 7 implementa el primer pipeline de audio sin TTS real.

## Núcleo

`NarrationScriptDocument` alimenta `AudioGenerationRequest`, que se procesa mediante `AudioGenerationGateway`. La implementación actual es `MockAudioGenerationGateway`.

## Salidas mock

```text
jobs/JOB-*/audio/SEG-001.wav
jobs/JOB-*/audio/SEG-002.wav
jobs/JOB-*/final/podcast-mock.wav
jobs/JOB-*/audio-manifest.json
jobs/JOB-*/logs/generation-log.jsonl
```

## UX

`AudioWorkspaceView` muestra progreso, ETA, segmento actual, carpeta del job y botones de generación/cancelación.

## Próximo paso

Conectar un gateway TTS real detrás del mismo puerto o fortalecer persistencia/reanudación antes de hacerlo.
