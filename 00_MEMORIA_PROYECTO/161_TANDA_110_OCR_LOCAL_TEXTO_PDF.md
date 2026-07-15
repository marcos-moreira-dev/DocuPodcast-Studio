# Tanda 110 - OCR local para detectar texto PDF

## Implementado

- Se agrego el contrato `PdfOcrEngine`.
- Se agregaron `PdfOcrRequest`, `PdfOcrPageResult`, `PdfOcrWord`, `PdfOcrLine`, `PdfOcrErrorCode` y `PdfOcrException`.
- Se agrego `BuildPdfOcrTextLayerUseCase` para exponer OCR por application boundary.
- Se implemento `PdfOcrTsvParser` para convertir TSV de Tesseract a palabras/lineas con bbox en puntos PDF y confianza.
- Se implemento `TesseractPdfOcrEngine`, usando `PdfRenderEngine` para renderizar pagina y `DefaultExternalProcessRunner` para ejecutar Tesseract CLI.
- El resultado OCR produce `PdfTextLayerOrigin.OCR_LOCAL`, pensado para alimentar lectura, generacion de voz, busqueda, resaltado e indice futuro.
- Se agrego cache TSV opcional por PDF/pagina/DPI/idioma.

## Fuera De Alcance

- Disparar OCR masivo automaticamente al abrir PDFs grandes.
- UI de configuracion fina de idioma OCR.
- OCRmyPDF o PaddleOCR como backend principal.
- Reconstruccion LaTeX o interpretacion matematica.

## Decisiones Tecnicas

- Tesseract v1 usa salida TSV porque trae bbox y confianza por palabra.
- OCR aplica a cualquier PDF cuando la politica lo solicite o cuando la capa nativa sea insuficiente; no queda limitado a PDFs escaneados.
- Si Tesseract no existe, el error es `TESSERACT_NOT_FOUND` y debe mostrarse como accion instalable en una tanda de UI.
- No se agrego `ProcessBuilder` nuevo fuera del runner comun.

## Archivos Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfOcr*.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildPdfOcrTextLayerUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/TesseractPdfOcrEngine.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfOcrTsvParserTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PdfVisualReadingOcrT109T110SourceTest.java`

## Tests Ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfOcrTsvParserTest,PdfVisualReadingOcrT109T110SourceTest" test`
- `mvn -q test`

## Proximo Paso

T111: rehacer indice/temario PDF usando bookmarks, Contents, capitulos/secciones y la capa textual nativa/OCR cuando haga falta, rechazando bibliografias pobres como indice principal.
