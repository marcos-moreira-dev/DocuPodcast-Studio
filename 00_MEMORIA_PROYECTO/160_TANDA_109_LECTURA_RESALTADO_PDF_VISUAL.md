# Tanda 109 - Lectura y resaltado sobre PDF visual

## Implementado

- Se agrego `BuildPdfVisualReadingProjectionUseCase` para unir `PdfTextLayer` nativa con highlights por bloque PDF.
- Se agregaron `PdfVisualReadingProjection` y `PdfVisualTextHighlight`.
- `PdfVisualDocumentView` ahora expone `showTextHighlight(...)` y `clearTextHighlight()`.
- El visor PDF pinta un rectangulo de resaltado sobre la pagina renderizada usando coordenadas PDF en puntos convertidas a coordenadas visibles del `ImageView`.
- `DocumentWorkspaceView` actualiza el highlight cuando cambia el cursor de reproduccion y sigue saltando a la pagina fuente.

## Fuera De Alcance

- OCR automatico desde UI.
- Resaltado palabra por palabra fino; la capa nativa actual trabaja principalmente por bloque/linea.
- Cambiar el flujo de audio de DOCX/Markdown/TXT.

## Decisiones Tecnicas

- Presentation no importa `PdfBoxRenderEngine`; consume `BuildPdfVisualReadingProjectionUseCase` desde application services.
- El resaltado es una capa independiente de la seleccion rectangular de regiones PDF.
- Si un bloque PDF no tiene `sourcePage + bbox`, se limpia el resaltado sin romper la lectura.

## Archivos Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfVisualReadingProjectionUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfVisualReadingProjection.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfVisualTextHighlight.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/resources/css/document/pdf-visual-viewer.css`

## Tests Ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfOcrTsvParserTest,PdfVisualReadingOcrT109T110SourceTest" test`
- `mvn -q test`

## Proximo Paso

T110 queda implementada en memoria separada: OCR local por pagina para producir capa textual interna utilizable por lectura/voz, busqueda e indice.
