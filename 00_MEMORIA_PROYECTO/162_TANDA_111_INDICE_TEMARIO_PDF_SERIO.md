# Tanda 111 - Indice y temario PDF serio

## Implementado

- Se agrego `BuildPdfEnhancedOutlineUseCase` para reforzar el indice PDF cuando el indice base es debil.
- El indice enriquecido usa la capa textual resuelta en paginas iniciales: texto nativo cuando existe y OCR local solo para paginas `UNAVAILABLE`.
- Las lineas sinteticas de OCR se usan para detectar `Contents/Table of Contents`, pero no se convierten en anclas visibles del indice.
- Se deduplican lineas de capa textual para evitar que encabezados, pies o OCR repetido inflen el temario.
- El flujo conserva prioridad de bookmarks, Contents nativo, secciones confiables y fallback por paginas PDF.
- Las inferencias pobres, bibliograficas o poco distribuidas siguen cayendo a `PDF_PAGES` en vez de presentarse como temario principal.
- `DocumentIndexPanel` aplica primero el indice base y luego intenta mejorar el indice PDF en segundo plano.

## Fuera De Alcance

- Redisenar visualmente el SideDock de estudio.
- OCR masivo automatico de libros completos.
- Detectar semantica matematica o reconstruir LaTeX.
- Editar el PDF fuente o cambiar `.docupodcast.json`.

## Decisiones Tecnicas

- La mejora de indice vive en `application.document`; presentation solo consume el caso de uso por `DocumentApplicationServices`.
- Si el indice base ya es fuerte, no se fuerza OCR.
- Si el indice enriquecido no produce una tabla de contenidos confiable, se conserva el indice base.
- Las anclas finales siguen apuntando a bloques reales o paginas existentes del documento, no a bloques OCR temporales.

## Archivos Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfEnhancedOutlineUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfResolvedTextLayerUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfResolvedTextLayerProjection.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfResolvedTextLayerRequest.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextResolutionPolicy.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`

## Tests Ejecutados

- `mvn -q "-Dtest=PdfResolvedSearchAndOutlineUseCaseTest,PdfOutlineSearchT111T112SourceTest,BuildDocumentOutlineUseCaseTest,PdfOcrTsvParserTest" test`
- `mvn -q -DskipTests compile`
- `mvn -q test`

## Proximo Paso

T112 queda implementada en memoria separada: busqueda PDF visual sobre capa textual nativa/OCR con salto y resaltado en el visor PDF.
