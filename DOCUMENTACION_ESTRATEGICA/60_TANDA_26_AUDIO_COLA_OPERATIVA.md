# Tanda 26 — Audio como cola operativa persistente

## Objetivo

Convertir el workspace Audio en una superficie operativa de producto: job activo, cola persistida, detalle, diagnósticos, manifest y segmentos del guion en una misma vista coherente.

## Cambios productivos

- Se agrega `presentation.audio.AudioQueueState` como proyección de cola persistente.
- Se agrega `presentation.audio.AudioJobRow` como fila uniforme para job activo y jobs persistidos.
- `DocuPodcastShellViewModel` expone `audioQueueState()` para que la UI no ensamble listas sueltas.
- `AudioWorkspaceView` se reorganiza como dashboard de cola:
  - job activo;
  - cola persistente;
  - detalle del job seleccionado;
  - diagnóstico TTS;
  - manifest de playback;
  - segmentos del guion.
- `renderStatus(...)` deja de leer jobs persistidos en cada evento de progreso.
- La cola se refresca manualmente y al llegar a estados terminales del job.

## Tests

- `AudioQueueStateTest`
- `AudioJobRowTest`
- `AudioWorkspaceSourceTest` reforzado

## Alcance

Esta tanda no cambia el gateway TTS ni la cancelación fuerte de procesos. Eso queda para Tanda 27.
