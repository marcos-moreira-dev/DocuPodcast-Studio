# Tanda 45 — Selección exacta por oración y rangos parciales

## Objetivo

Permitir que el Documento narrado deje de operar solo por bloque/párrafo y pueda usar una oración concreta como ancla para voz, audio, emoción, imagen o ambiente. La selección se guarda como capa del proyecto DocuPodcast: no se escribe ni se acota dentro del Word original.

## Cambios principales

- Agrega `DocumentTextRange`, `DocumentSentenceSpan` y `DocumentSentenceSplitter` en dominio documental.
- `DocumentWorkspaceView` renderiza las oraciones de cada bloque con `TextFlow` y nodos clicables.
- Al hacer clic en una oración, se llama `selectDocumentTextRange(...)` en el ViewModel.
- `DocuPodcastShellViewModel` mantiene `selectedDocumentTextRange` y `selectedDocumentRangeLabel`.
- El rail de capas usa la etiqueta exacta de selección: bloque completo u oración.
- `prepareDocumentLayerAssignment(...)` calcula, cuando es posible, el `ScriptTextRange` equivalente para preparar la capa sobre el guion narrable.
- Se agrega resaltado `.document-sentence-selected`.

## Alcance

Esta tanda prepara selección exacta y capa operativa. No persiste todavía asignaciones definitivas ni implementa drag/selección libre con arrastre. La selección actual es por oración detectada de forma conservadora.

## Validación agregada

- `DocumentSentenceSplitterTest`
- `DocumentExactTextSelectionSourceTest`

## Límites explícitos

- No modifica el DOCX original.
- No escribe instrucciones como “decir enojada” dentro del texto.
- No genera todavía audio diferente por rango.
- No reemplaza la futura persistencia completa de asignaciones.
