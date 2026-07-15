# Tanda 54 — Mini rail multimedia real

## Objetivo

Hacer que el mini rail del Documento muestre en un solo lugar las capas activas, el mini storyboard y las imágenes importadas, manteniéndose plegable/retraíble y sin convertirse en cabina técnica.

## Cambios

- `DocumentMediaRailView` agrega sección `Capas activas`.
- Las capas guardadas se renderizan como tarjetas compactas.
- Clic en una capa llama a `selectNarrativeLayerAssignment(...)` y vuelve al texto asociado.
- El rail sigue mostrando mini storyboard, imágenes asignadas e imágenes sin texto asignado.
- `DocumentLayerRailView` queda enfocado en acciones, no duplica la lista de capas.

## Tests

- `MiniMediaRailRealAssignmentsSourceTest`
- ajustes de `DocumentLayerAssignmentWorkflowSourceTest`
