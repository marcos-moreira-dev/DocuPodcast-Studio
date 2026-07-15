# 27 — Tanda 4 plan: Guion narrable

## Objetivo

Convertir documento importado a guion narrable editable.

## Dominio

```text
NarrationScriptDocument
ScriptSection
NarrationSegment
ScriptSelection
PerformanceSpan
ScriptValidationIssue
```

## Use cases

```text
BuildNarrationScriptUseCase
SplitNarrationSegmentUseCase
MergeNarrationSegmentsUseCase
UpdateNarrationSegmentUseCase
ValidateNarrationScriptUseCase
```

## UI

```text
ScriptWorkspaceView
ScriptDocumentView
ScriptSegmentsPanel
ScriptPropertiesPanel
ScriptValidationPanel
```

## MVP

Selección por segmento completo. Rango de texto después.

## Tests

```text
NarrationScriptDocumentTest
ScriptValidationServiceTest
ScriptWorkspaceNoCanvasSourceTest
```
