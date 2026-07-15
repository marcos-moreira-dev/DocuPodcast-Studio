# Tanda 112 - Buscador PDF visual

## Implementado

- Se agrego `SearchPdfTextUseCase` para buscar en la capa textual PDF resuelta.
- Se agregaron `PdfTextSearchRequest`, `PdfTextSearchProjection` y `PdfTextSearchResult`.
- La busqueda normal usa texto nativo disponible y no dispara OCR pesado.
- La accion explicita `Buscar con OCR local` puede completar paginas sin capa textual usando cache por proyecto/PDF/pagina.
- Cada resultado incluye pagina, origen de texto, contexto, bloque asociado cuando existe y `PdfVisualTextHighlight`.
- `DocumentIndexPanel` incorpora busqueda PDF en el panel Indice: campo de busqueda, accion normal, accion OCR explicita, limpiar y estado.
- Al seleccionar un resultado, `DocumentWorkspaceView` solicita resaltado visual sobre `PdfVisualDocumentView` y salta a la pagina correspondiente.

## Fuera De Alcance

- Busqueda incremental indexada para miles de paginas.
- UI avanzada de filtros por pagina/capitulo.
- Forzar OCR de todo el libro desde la busqueda normal.
- Persistir indices de busqueda en `.docupodcast.json`.

## Decisiones Tecnicas

- El buscador vive en `application.document` y reusa `BuildPdfResolvedTextLayerUseCase`.
- Presentation no importa infraestructura PDFBox ni Tesseract directamente.
- El OCR para busqueda queda opt-in desde la accion especifica, para evitar trabajo masivo silencioso.
- Si no hay capa textual ni OCR disponible, la busqueda devuelve diagnostico en el panel sin romper el visor.

## Archivos Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/SearchPdfTextUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextSearchRequest.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextSearchProjection.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextSearchResult.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/resources/css/document-reader.css`
- `src/main/resources/css/document/pdf-index-search.css`
- `src/main/resources/css/docupodcast-light.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfResolvedSearchAndOutlineUseCaseTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PdfOutlineSearchT111T112SourceTest.java`

## Tests Ejecutados

- `mvn -q "-Dtest=PdfResolvedSearchAndOutlineUseCaseTest,PdfOutlineSearchT111T112SourceTest,BuildDocumentOutlineUseCaseTest,PdfOcrTsvParserTest" test`
- `mvn -q -DskipTests compile`
- `mvn -q test`

## Proximo Paso

T113: adaptar el SideDock de estudio a paginas/regiones PDF para que Indice, Problema, Lectura y Fragmento trabajen con el visor visual y no solo con bloques de texto.
