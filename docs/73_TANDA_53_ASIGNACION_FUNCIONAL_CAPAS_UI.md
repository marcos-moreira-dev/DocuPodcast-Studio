# Tanda 53 — Selección y asignación funcional desde UI

## Objetivo

Convertir la selección de oración/bloque del Documento en una acción real de proyecto: asignar voz IA, audio principal, emoción, imagen/storyboard o audio ambiente como capa persistible, sin escribir acotaciones dentro del Word original.

## Cambios

- `DocuPodcastShellViewModel.prepareDocumentLayerAssignment(...)` crea `NarrativeLayerAssignment` y la agrega al `DocuPodcastProject`.
- Se mantiene la protección de conflictos mediante `NarrativeLayerAssignmentPolicy`.
- Si un rango ya tiene voz o audio principal, la UI informa que debe reemplazar, dividir o desasignar.
- Se agrega `removePrimaryAssignmentForSelectedDocumentRange()` para quitar la voz/audio principal del rango seleccionado.
- Se agrega `selectNarrativeLayerAssignment(...)` para volver desde una capa guardada al texto/documento asociado.
- Se agrega `DocumentLayerAssignmentPresentation` como proyección de UI.

## Tests

- `DocumentLayerAssignmentWorkflowSourceTest`
- `DocumentLayerAssignmentPresentationTest`
