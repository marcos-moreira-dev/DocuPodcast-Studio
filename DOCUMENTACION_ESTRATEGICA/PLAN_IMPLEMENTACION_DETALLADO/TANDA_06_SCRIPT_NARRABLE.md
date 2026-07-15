# Tanda 6 — Guion narrable

## Objetivo

Crear `NarrationScriptDocument` desde `ReadableDocument`.

## Dominio

- `NarrationScriptDocument`;
- `ScriptSection`;
- `NarrationSegment`;
- `ScriptSelection`;
- `ScriptValidationIssue`.

## Application

- `BuildNarrationScriptUseCase`;
- `SplitNarrationSegmentUseCase`;
- `MergeNarrationSegmentsUseCase`;
- `UpdateNarrationSegmentTextUseCase`;
- `ValidateNarrationScriptUseCase`.

## UI

- Script workspace;
- lista de segmentos;
- editor central;
- inspector de segmento;
- validación.

## Criterios

- Generar segmentos desde documento.
- Editar texto de segmento.
- Marcar segmento ignorado.
- Guardar/reabrir guion.
