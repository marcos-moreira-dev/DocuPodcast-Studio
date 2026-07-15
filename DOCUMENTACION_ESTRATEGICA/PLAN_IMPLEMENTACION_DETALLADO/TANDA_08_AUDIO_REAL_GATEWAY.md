# Tanda 8 — Audio real Gateway

## Objetivo

Conectar motor TTS real detrás de `AudioGenerationGateway`.

## Opciones

- Python worker empaquetado;
- Piper process;
- ONNX runtime;
- otro motor local.

## Regla

La UI no sabe qué motor se usa.

## Entregables

- `TtsEngineGateway`;
- `VoiceEngineDiagnostics`;
- `AudioSegmentGenerator`;
- `AudioMerger`;
- WAV por segmento;
- WAV final.

## Criterios

- Genera audio real de segmentos cortos.
- Maneja error por segmento.
- Reporta logs.
- No bloquea JavaFX.
