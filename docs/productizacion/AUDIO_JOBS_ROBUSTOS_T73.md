# Audio jobs robustos — T73

## Intención

DocuPodcast Studio genera audio por fragmentos. Eso permite comenzar playback con buffer, reanudar trabajos y evitar regenerar todo innecesariamente. Pero para que el producto sea confiable, el cerebro debe entender el estado real de esos jobs.

## Contrato nuevo

`InspectAudioJobMaintenanceUseCase` inspecciona un `AudioJobSnapshot` y el proyecto físico para decidir:

- si se puede reutilizar audio existente;
- si el job admite reanudación;
- si faltan WAVs que deberían existir;
- si el documento fuente cambió y el audio queda obsoleto;
- qué segmentos quedan afectados.

## Estados

- `READY`: todo está completo y los audios existen.
- `RESUMABLE`: hay segmentos pendientes/fallidos/cancelados que pueden reanudarse.
- `STALE_SOURCE`: el documento fuente cambió; el audio obsoleto debe regenerarse.
- `MISSING_AUDIO`: el job dice que hay audio, pero el archivo no existe.
- `EMPTY_JOB`: el job no contiene segmentos.
- `REVIEW_REQUIRED`: estado no terminal que requiere inspección manual.

## Relación con Refrescar contenido

Cuando `RefreshSourceDocumentUseCase` reporta cambios, el audio no se elimina. Se marca como obsoleto y el usuario/flujo posterior decide si debe regenerarse.

## Qué NO hace esta tanda

- No implementa un motor TTS nuevo.
- No concatena audio final real.
- No rediseña la UI de Audio.
- No borra jobs fallidos automáticamente.

## Siguiente paso

T74 debe cerrar capas narrativas reales, eliminando placeholders de voz/imagen/audio donde todavía existan.
