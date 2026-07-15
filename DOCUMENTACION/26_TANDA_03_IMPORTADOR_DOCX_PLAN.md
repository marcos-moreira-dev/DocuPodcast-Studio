# 26 — Tanda 3 plan: Importador DOCX

## Objetivo

Aceptar Word/DOCX como fuente primaria.

## Dependencias esperadas

Apache POI para DOCX.

## Dominio

```text
ReadableDocument
DocumentBlock
DocumentBlockType
DocumentImageNotice
DocumentTableNotice
DocumentMetadata
ReadingProfile
HeadingDetectionRules
```

## Infraestructura

```text
DocxDocumentImporter
DocxStyleInspector
DocxImageExtractor
```

## Presentation

```text
DocumentWorkspaceView
DocumentReaderView
DocumentStructurePanel
DocumentPropertiesPanel
DocumentDiagnosticsPanel
```

## Tests

```text
DocxDocumentImporterTest
DocumentStructureDetectionTest
WordFirstProductContractSourceTest
```

## Criterio de cierre

Abrir DOCX de prueba, extraer bloques, mostrar con scroll y generar diagnóstico básico.
