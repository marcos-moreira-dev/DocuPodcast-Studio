# Tanda 8 — Estado de implementación

## Completado

- Gateway TTS real por proceso local.
- Configuración por propiedades JVM y variables de entorno.
- Descriptores de motor visibles.
- Selección automática mock/real en `InfrastructureServicesFactory`.
- Job por segmentos con `segments/*.txt`, `audio/*.wav`, `audio-manifest.json`, logs y snapshots.
- Tests de configuración, descriptor y contrato fuente.

## No incluido

- Binarios XTTS/Piper.
- Modelos de voz.
- UI completa de configuración de motor.
- Unión real de WAVs con FFmpeg.
- Voice Library.

## Siguiente recomendado

Tanda 9 — Reanudación avanzada de jobs con motor real y diagnóstico de proceso.
