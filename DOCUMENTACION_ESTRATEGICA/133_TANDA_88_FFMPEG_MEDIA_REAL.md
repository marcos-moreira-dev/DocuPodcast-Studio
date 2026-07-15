# Estrategia — Tanda 88 FFmpeg/media real

T88 es una tanda de cierre de cerebro/media, no de estética. La prioridad fue que la opción `Audio del computador` funcione con archivos reales dentro del proyecto portable.

## Decisiones

- FFmpeg es el gateway operativo para normalización de audio no WAV y extracción de audio desde video.
- Los detalles técnicos quedan como sidecars de diagnóstico, no como texto visible en Documento.
- El usuario opera desde el inspector izquierdo; el rail derecho sigue siendo visual/navegacional.
- No se promete todavía sustitución de playback por audio humano hasta modelar `AudioUnit`.

## Próxima frontera

T89 debe transformar Configuración en preflight accionable de motores. T91/T92 deben cerrar render narrativo por oración y anclas estables.
