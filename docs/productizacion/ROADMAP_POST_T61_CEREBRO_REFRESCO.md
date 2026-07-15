# Roadmap post T61 — cerebro y refresco de fuente

## Base estable

T61 agrega la primera pieza ejecutable del cerebro para `Refrescar contenido`: snapshot, comparación y reporte.

## T62 — Refactor prioritario de coordinadores

Objetivo: reducir `DocuPodcastShellViewModel` sin cambiar la experiencia visible.

Prioridad:

1. `SourceDocumentRefreshCoordinator`.
2. `DocumentIntakeCoordinator`.
3. `NarratedDocumentCoordinator`.
4. `PlaybackWorkflowCoordinator`.
5. `AudioWorkflowCoordinator`.
6. `WorkspaceNavigationCoordinator`.

## T63 — Round-trip funcional real

Validar:

- abrir documento;
- generar Documento narrable;
- escuchar;
- refrescar fuente modificada externamente;
- marcar audio obsoleto;
- conservar capas en revisión;
- guardar;
- cerrar;
- reabrir;
- ver el estado de obsolescencia.

## T64 — Configuración operativa

Convertir configuración en settings persistentes:

- buffer;
- TTS;
- STT;
- FFmpeg;
- carpetas;
- diagnóstico.

## T65 — Rediseño UI aplicado

Aplicar cara más limpia solo cuando el cerebro tenga contratos estables.
