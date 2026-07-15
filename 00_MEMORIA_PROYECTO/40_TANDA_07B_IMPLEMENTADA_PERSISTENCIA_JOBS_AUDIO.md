# Memoria — Tanda 7B persistencia de jobs de audio

La Tanda 7B convierte la cola mock de audio en un flujo con evidencia persistente por job. Esta decisión fue necesaria antes de conectar un motor TTS real, porque la generación de documentos grandes puede durar minutos y no debe depender solo de estado en memoria.

## Decisiones

1. `AudioJobSnapshot` es el contrato principal de recuperación.
2. `AudioSegmentSnapshot` conserva el estado de cada segmento.
3. `AudioJobFileRepository` escribe `job.json` y `segments-status.json` bajo `jobs/JOB-*`.
4. El gateway mock persiste snapshots durante el procesamiento.
5. La UI puede listar historial persistido.
6. Un job encontrado como activo al reabrir se muestra como interrumpido para evitar estados fantasma.

## Pendiente recomendado

Tanda 7C — Recuperación/reintento operativo de jobs: UI de jobs persistidos, botón reintentar pendientes/fallidos y reaprovechamiento de WAVs completados.
