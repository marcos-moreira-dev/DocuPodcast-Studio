# TI6 — FFmpeg como job persistente/cancelable

## Propósito

TI6 convierte el render de storyboard/video en un contrato de job persistente. Hasta ahora DocuPodcast generaba un paquete auditable con comandos FFmpeg; desde TI6 también existe un snapshot de job que puede listarse, diagnosticarse y cancelarse sin depender de una ventana efímera.

## Contrato operativo

- El paquete de storyboard/video sigue siendo auditable.
- El render FFmpeg se representa como `VIDEO_RENDER` dentro del contrato común de procesos largos.
- El job se guarda en `jobs/video/<jobId>/video-render-job.json`.
- Los logs previstos quedan en `jobs/video/<jobId>/logs/`.
- La cancelación segura escribe `cancel.requested` y conserva paquete, comandos y manifest.
- La UI puede bloquear nuevas exportaciones de video mientras el job está activo.

## Clases agregadas

- `VideoRenderJobSnapshot`.
- `VideoRenderJobRepository`.
- `SubmitVideoRenderJobUseCase`.
- `CancelVideoRenderJobUseCase`.
- `VideoRenderJobProcessMapper`.
- `VideoRenderJobFileRepository`.

## Integración con procesos largos

`ProcessJobKind.VIDEO_RENDER` queda marcado como familia persistente. `ListProcessJobsUseCase` puede combinar jobs de audio TTS y jobs de render de video, ordenados por fecha de actualización.

## Cancelación

La cancelación de TI6 es deliberadamente segura y conservadora:

1. Se marca el job como cancelado.
2. Se escribe `cancel.requested`.
3. Se conserva el paquete de video.
4. Se conservan `render-commands.txt` y `RENDER_MANIFEST.json`.
5. El usuario puede reintentar más adelante.

La ejecución real de proceso FFmpeg con control de proceso vivo puede reforzarse después, pero el contrato persistente ya queda preparado.

## Regla visual relacionada

El video nace de `RenderUnitPlan.videoUnits()`, no de todos los segmentos. Las unidades visuales silenciosas usan silencio sintético y no fuerzan narración.

## Ubicación de fragmentos

Como mejora de contexto, el sidebar de Fragmento muestra una ubicación de fuente pequeña:

- DOCX: bloque Word/DOCX, con aviso de paginación dinámica.
- PDF: bloque PDF nativo, con aviso de que la página exacta no está disponible en el extractor V1.
- TXT/Markdown: línea o rango de líneas cuando aplica.

Esto evita mezclar el bloque interno de DocuPodcast con la ubicación percibida del documento original.

## No narrar código visual

Los elementos visuales no renderizables, como tablas, fórmulas, bloques matemáticos o LaTeX detectado, no se leen en voz por defecto. Se identifican como bloque visual fuente y el usuario decide si les asigna una imagen para storyboard/video.
