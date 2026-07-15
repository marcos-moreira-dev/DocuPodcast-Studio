# Tanda 103 - Seleccion rectangular sobre PDF

## Que se implemento

- Se expuso `CapturePdfVisualRegionUseCase` desde `DocumentApplicationServices`.
- `ApplicationServicesFactory` cablea `CapturePdfVisualRegionUseCase` con el `PdfBoxRenderEngine` embebido.
- `PdfVisualDocumentView` ahora soporta modo de seleccion rectangular:
  - overlay por pagina renderizada;
  - arrastre sobre la imagen visible;
  - umbral minimo para evitar capturas accidentales;
  - conversion desde coordenadas visibles del `ImageView` a `PdfViewportSelection`.
- `DocumentWorkspaceView` acumula capturas PDF temporales como `PdfRegionCaptureDraft`.
- Cada captura se renderiza en segundo plano con `CapturePdfVisualRegionUseCase` y queda identificada como `PDFREG-0001`, `PDFREG-0002`, etc.
- El estado temporal se limpia al cambiar de PDF.

## Que quedo fuera

- Persistencia final de problemas PDF por capturas queda en T104.
- OCR, busqueda, resaltado de lectura y temario PDF avanzado siguen fuera.
- No se implemento seleccion multiple con edicion de rectangulos antes de capturar.

## Decisiones tecnicas

- La UI usa contratos de `application.document`, pero no importa `PdfBoxRenderEngine`.
- El render del crop corre fuera del hilo JavaFX mediante `Task`.
- La region se calcula sobre el tamano visible de la pagina renderizada, no sobre el `ScrollPane`.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfRegionCaptureDraft.java`
- `src/main/resources/css/document/pdf-visual-viewer.css`
- Tests de source/guardarrailes relacionados con PDF visual.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfVisualRegionCaptureUseCaseTest,PdfRegionTechnicalProblemT103T104SourceTest,PdfVisualWorkspaceT102SourceTest,PdfVisualComfyT100SourceTest,StudyProblemWorkflowTest,DocuPodcastProjectFileRepositoryTest,DocumentWorkspaceViewSourceTest" test`
- `mvn -q test`

## Proximos pasos

- T104: guardar problemas tecnicos PDF desde capturas rectangulares como `STUDY_SOURCE_CROP`.
- Mantener DOCX/Markdown/TXT con seleccion por checkboxes de bloque.
