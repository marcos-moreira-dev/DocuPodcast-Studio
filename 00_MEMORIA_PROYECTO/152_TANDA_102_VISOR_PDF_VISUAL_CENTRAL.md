# Tanda 102 - Visor PDF Visual Central

## Que se implemento

- Se agrego `RenderPdfVisualPageUseCase` como frontera de aplicacion para renderizar paginas PDF sin acoplar presentation a PDFBox.
- `DocumentApplicationServices` expone `BuildPdfVisualDocumentUseCase` y `RenderPdfVisualPageUseCase`.
- `ApplicationServicesFactory` crea una instancia compartida de `PdfBoxRenderEngine` para el flujo visual PDF.
- Se creo `PdfVisualDocumentView` con scroll vertical, render diferido de paginas cercanas, cache LRU simple, descarga de imagenes lejanas y placeholder por error de pagina.
- `DocumentWorkspaceView` usa el visor visual central cuando `ReadableDocument.format()` es PDF.
- DOCX, Markdown y TXT conservan el lector por bloques existente.
- La navegacion desde bloque/indice en PDF salta a la pagina fuente mediante `sourcePage`.

## Que quedo fuera

- Seleccion rectangular PDF.
- OCR.
- Busqueda visual.
- Resaltado de lectura sobre pagina.

## Decisiones tecnicas

- El visor debe depender de casos de uso de `application.document`, no de `PdfBoxRenderEngine` directamente desde presentation.
- DOCX, Markdown y TXT conservaran el lector por bloques.

## Archivos/sistemas previstos

## Archivos/sistemas tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/RenderPdfVisualPageUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/resources/css/document/pdf-visual-viewer.css`
- `src/main/resources/css/docupodcast-light.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualWorkspaceT102SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/document/RenderPdfVisualPageUseCaseTest.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=RoadmapStudyPdfPostT100Test,PdfVisualWorkspaceT102SourceTest,RenderPdfVisualPageUseCaseTest" test`
- `mvn -q "-Dtest=CssModularitySourceTest,DocumentPrimaryOperationSourceTest,FloatingReadingControlSourceTest,PdfVisualWorkspaceT102SourceTest" test`
- `mvn -q test`

## Proximo paso exacto

T103: cache/rendimiento PDF con limites por memoria, precarga cercana y metrica de diagnostico.
