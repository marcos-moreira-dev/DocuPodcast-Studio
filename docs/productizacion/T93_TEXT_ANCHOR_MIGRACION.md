# T93 — TextAnchor mínimo y migración controlada

## Objetivo

T93 introduce un ancla textual más robusta para las capas narrativas sin romper proyectos previos. Hasta T92, una capa guardaba solo `sourceBlockId`, `sourceStartOffset` y `sourceEndOffset`. Eso funciona si el documento no cambia, pero no alcanza para reconciliar asignaciones cuando el usuario refresca un DOCX/PDF/TXT/Markdown modificado.

## Cambios principales

- Nuevo dominio documental:
  - `TextAnchor`
  - `TextAnchorConfidence`
  - `TextAnchorStatus`
- `NarrativeLayerAssignment` conserva `DocumentTextRange`, pero ahora también puede llevar `TextAnchor`.
- El JSON de proyecto sube a `formatVersion = 2`.
- El lector sigue aceptando proyectos v1.
- Al leer una capa antigua v1 con rango documental simple, se crea un `TextAnchor` legacy con:
  - `confidence = LOW`
  - `status = NEEDS_REVIEW`
- El writer mantiene los campos legacy `sourceBlockId/sourceStartOffset/sourceEndOffset` y además escribe `textAnchor`.

## Alcance honesto

T93 no implementa todavía reconciliación granular automática ni diff por bloque. Deja el contrato persistente y compatible para que una tanda posterior pueda comparar texto, hash y contexto.

## Regla de producto

El documento fuente sigue siendo solo lectura. `TextAnchor` es metadata del proyecto DocuPodcast; no modifica Word/PDF/TXT/Markdown.

## Validación focal

- `TextAnchorTest`
- `DocuPodcastProjectNarrativeLayersJsonTest`
- `DocuPodcastProjectFileRepositoryTest`
- `TextAnchorMigrationT93SourceTest`
