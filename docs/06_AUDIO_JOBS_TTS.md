# Audio jobs y motor TTS

## Decisión

El audio se genera por segmentos. No se procesa un documento entero de una vez.

## Flujo

```text
NarrationScriptDocument
  → AudioGenerationPlan
  → AudioJob
  → WAV por segmento
  → AudioManifest
  → podcast final
```

## Requisitos

- Progreso visible.
- ETA después de varios segmentos.
- Cancelación cooperativa.
- No borrar audios parciales.
- Reintentar segmentos fallidos.
- Reanudar desde job persistido.

## Motor TTS

El motor se encapsula detrás de `AudioGenerationGateway`.

Implementaciones posibles:

- `MockAudioGenerationGateway` para UI/tests.
- `XttsPythonWorkerGateway` si la calidad exige Python empaquetado.
- `PiperProcessGateway` como motor liviano.
- `OnnxRuntimeGateway` si resulta viable.

La UI no conoce la implementación concreta.
