# Tanda 3 cerrada — Importador Word/DOCX + diagnóstico documental

Esta tanda convierte el importador DOCX mínimo en una primera pieza robusta para notas reales en Word.

## Implementado

- Lectura de `word/document.xml` en orden del cuerpo del documento.
- Lectura opcional de `word/styles.xml` para mapear `styleId` a nombre visible de estilo.
- Lectura opcional de `docProps/core.xml` para usar el título del documento si existe.
- Detección inicial de:
  - títulos;
  - subtítulos;
  - párrafos;
  - listas numeradas o con `numPr`;
  - tablas simples;
  - imágenes embebidas;
  - descripción de imagen mediante `wp:docPr` cuando exista.
- Diagnóstico de importación mediante `DocumentImportReport`.
- Advertencias para imágenes sin descripción, párrafos largos y documentos planos sin títulos detectados.

## Decisión importante

Se sigue usando ZIP/XML del JDK para evitar introducir Apache POI todavía. Si los DOCX reales del usuario empiezan a exigir más fidelidad, Apache POI puede reemplazar el adaptador sin romper la capa de aplicación porque existe el puerto `DocumentImporter`.

## Resultado

El flujo real queda:

```text
DOCX → DocxDocumentImporter → ReadableDocument → DocumentWorkspace → document/document.json
```

Word/DOCX queda confirmado como entrada prioritaria desde el MVP.
