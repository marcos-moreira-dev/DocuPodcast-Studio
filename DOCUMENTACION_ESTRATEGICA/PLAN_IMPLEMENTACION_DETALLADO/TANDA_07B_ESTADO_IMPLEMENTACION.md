# Tanda 7B — Estado de implementación

## Estado

Implementada.

## Alcance cerrado

- Dominio `AudioJobSnapshot` y `AudioSegmentSnapshot`.
- Puerto `AudioJobRepository`.
- Adaptador `AudioJobFileRepository`.
- Caso de uso `ListPersistedAudioJobsUseCase`.
- Persistencia desde `MockAudioGenerationGateway`.
- Historial persistido visible en workspace Audio.
- Tests de snapshot, repository y gateway.

## Fuera de alcance

- Reanudar generación automáticamente.
- Reintentar pendientes/fallidos.
- Leer `audio-manifest.json` como dominio completo.
- Integrar TTS real.

## Tanda opcional agregada

`Tanda 7C — Recuperación operativa de jobs persistidos`.

Objetivos:

1. Mostrar detalle de job persistido.
2. Distinguir completados, pendientes, cancelados y fallidos.
3. Reintentar pendientes/fallidos usando mock.
4. No regenerar clips completados.
5. Preparar el mismo flujo para TTS real.
