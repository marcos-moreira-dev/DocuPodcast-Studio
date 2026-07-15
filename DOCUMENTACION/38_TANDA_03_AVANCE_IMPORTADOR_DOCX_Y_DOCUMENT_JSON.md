# Tanda 3 avance — DOCX real mínimo y `document/document.json`

Además de la UI de proyecto, se adelantó parte de la Tanda 3:

- `DocxDocumentImporter` extrae texto desde `word/document.xml` con ZIP/XML del JDK.
- Se crearon `ReadableDocument`, `DocumentBlock`, `DocumentBlockType` y `SourceDocumentFormat`.
- Al guardar un proyecto con DOCX importado, se materializa:
  - `source/<archivo>.docx`
  - `document/document.json`
  - assets `SRC-001` y `DOC-001` en `.docupodcast.json`

## Limitaciones

- El extractor no reconstruye formato Word completo.
- La clasificación de títulos/subtítulos es inicial.
- Falta SideDock de estructura documental y Reading Profile configurable.

## Valor

El flujo Word-first ya no es solo promesa: existe una primera cadena real DOCX → bloques → workspace → guardar proyecto con fuente y snapshot documental.
