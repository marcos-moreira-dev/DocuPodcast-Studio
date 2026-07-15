# 10 — Word/DOCX como entrada prioritaria

El usuario tiene sus notas principalmente en Word. Por tanto, DOCX es entrada de primera clase desde el MVP.

## Flujo principal

```text
Abrir DOCX
  → extraer estructura
  → mostrar documento
  → ajustar perfil de lectura
  → crear guion narrable
```

## Qué extraer

- Párrafos.
- Títulos y subtítulos por estilos Word.
- Fallback por tamaño, negrita y numeración.
- Listas simples.
- Tablas simples.
- Imágenes como avisos.
- Alt text/descripción de imagen cuando exista.

## Qué no hacer inicialmente

- No editar el DOCX original.
- No interpretar imágenes con IA.
- No garantizar orden perfecto en documentos corruptos.
- No manejar `.doc` antiguo como prioridad.

## Componentes esperados

```text
DocxDocumentImporter
ReadableDocument
DocumentBlock
DocumentBlockType
DocumentImageNotice
ReadingProfile
HeadingDetectionRules
DocumentWorkspaceView
DocumentStructurePanel
DocumentDiagnosticsPanel
```

## Tests obligatorios

```text
DocxDocumentImporterTest
DocumentStructureDetectionTest
WordFirstProductContractSourceTest
GuideWordFirstSourceTest
```

## Guía

Debe existir una página de ayuda “Importar notas desde Word” y aparecer antes que Markdown/IA en el flujo de primeros pasos.
