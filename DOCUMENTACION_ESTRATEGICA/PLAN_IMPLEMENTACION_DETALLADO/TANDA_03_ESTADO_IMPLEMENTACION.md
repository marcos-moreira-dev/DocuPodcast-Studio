# Tanda 03 — Estado de implementación

## Estado

Tanda 3 queda implementada en una versión técnica suficiente para el MVP temprano: DocuPodcast Studio ya tiene importador Word/DOCX real basado en APIs JDK, sin Apache POI todavía, con preservación de orden del cuerpo del documento y diagnósticos visibles.

## Implementado

- `DocxDocumentImporter` lee `word/document.xml` desde el `.docx`.
- Procesa el cuerpo del documento en orden real: párrafos y tablas intercalados.
- Clasifica bloques en `TITLE`, `HEADING`, `SUBHEADING`, `PARAGRAPH`, `LIST_ITEM`, `IMAGE_NOTICE`, `TABLE_NOTICE` y `EMPTY`.
- Detecta estilos Word como `Title`, `Heading1`, `Heading2`, `Título1`, `Título2`.
- Detecta listas por `w:numPr` y estilos tipo lista/bullet.
- Aplica heurísticas moderadas para subtítulos: numeración, línea corta en negrita y fuente grande.
- Detecta imágenes mediante `w:drawing` / `w:pict` y toma descripción desde `wp:docPr` cuando existe.
- Detecta imágenes sin descripción y emite advertencia.
- Convierte tablas en avisos narrables con filas/columnas y preview del contenido.
- Genera diagnósticos de importación en `ReadableDocument.diagnostics()`.
- Guarda los diagnósticos y metadata de bloques en `document/document.json`.

## Nuevos tipos de dominio

- `DocumentImportDiagnostic`
- `DocumentDiagnosticKind`
- `DiagnosticSeverity`

## Métricas añadidas a `ReadableDocument`

- conteo de títulos/subtítulos;
- conteo de párrafos;
- conteo de listas;
- conteo de imágenes;
- conteo de tablas;
- conteo aproximado de palabras;
- conteo de advertencias y errores.

## Limitaciones conscientes

- Aún no hay Apache POI.
- No se resuelve layout Word complejo.
- No se extrae aún toda la riqueza de estilos/temas/relaciones.
- Las tablas se resumen, no se narran celda por celda.
- Las imágenes se notifican; no hay lectura visual por IA.

## Criterio de producto

DOCX sigue siendo entrada prioritaria. Markdown será puente IA/humano, pero Word es el flujo principal de notas del usuario.
