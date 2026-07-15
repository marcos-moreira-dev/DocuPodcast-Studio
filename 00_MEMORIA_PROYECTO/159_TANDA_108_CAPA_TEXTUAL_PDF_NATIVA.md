# Tanda 108 - Capa textual PDF nativa

## Implementado

- Se agrego `BuildPdfNativeTextLayerUseCase` en `application.document`.
- El caso de uso construye `PdfTextLayer` desde bloques PDF con `sourcePage`, `bbox`, `bboxUnits=pdf-points`, `pageWidth` y `pageHeight`.
- Cada linea nativa queda marcada como `NATIVE_BBOX`, con confianza y tokens aproximados.
- Las paginas conocidas sin bbox nativo quedan como `UNAVAILABLE` con warning.
- `DocumentApplicationServices` y `ApplicationServicesFactory` exponen el nuevo caso de uso.

## Quedo fuera

- OCR local queda para T110.
- Resaltado/lectura sobre PDF visual queda para T109.
- Busqueda PDF visual queda para T112.

## Decisiones tecnicas

- La capa textual se calcula desde `ReadableDocument`; no importa PDFBox ni infraestructura.
- Los tokens son aproximados porque la importacion actual guarda bbox por bloque/linea, no por palabra.
- No se cambia `.docupodcast.json` ni la interfaz grafica.

## Archivos/sistemas tocados

- `BuildPdfNativeTextLayerUseCase`
- `DocumentApplicationServices`
- `ApplicationServicesFactory`
- Documentacion PDF y roadmap.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=StudyProblemExportT107SourceTest,BuildPdfNativeTextLayerUseCaseTest,PdfNativeTextLayerT108SourceTest,StudyDocumentUxT98SourceTest,PdfRegionTechnicalProblemT103T104SourceTest,StudyProblemWorkflowTest,PdfVisualComfyT100SourceTest" test`
- `mvn -q test`

## Proximos pasos

- T109: lectura y resaltado sobre PDF visual usando `PdfTextLayer` cuando exista capa nativa disponible.
