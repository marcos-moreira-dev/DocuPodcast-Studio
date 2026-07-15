# Tanda 107 - Exportacion PNG premium

## Implementado

- Se agrego una API de exportacion para el lienzo tecnico: `StudyProblemCanvasExportOptions` y `StudyProblemCanvasExportResult`.
- La exportacion externa intenta escala 4x, recorta al area util con margen y baja a 2x/1x si excede el limite seguro de pixeles.
- La composicion mantiene fondo opaco, imagenes transferidas y tinta encima para evitar transparencias o zonas negras.
- `Guardar + exportar PNG...` usa una imagen premium externa separada del asset interno del proyecto.
- La persistencia interna conserva una salida segura para `STUDY_SOLUTION_IMAGE`.

## Quedo fuera

- UI para escoger manualmente 1x/2x/4x queda fuera; v1 usa `AUTO`.
- Crop interactivo manual queda fuera; se usa recorte util calculado por contenido.

## Decisiones tecnicas

- La salida interna usa `internalPersistence()` y la externa usa `premiumExternal()`.
- Si 4x no cabe en `maxPixelCount`, el resultado incluye warning de degradacion.
- La escritura externa recibe directamente el `WritableImage` premium; no copia el asset interno.

## Archivos/sistemas tocados

- `StudyProblemCanvasSurface`
- `TechnicalProblemDialog`
- `DocumentWorkspaceView`
- `DocuPodcastShellViewModel`
- `StudyProblemWorkflow`
- Documentacion de estudio documental y roadmap.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=StudyProblemExportT107SourceTest,BuildPdfNativeTextLayerUseCaseTest,PdfNativeTextLayerT108SourceTest,StudyDocumentUxT98SourceTest,PdfRegionTechnicalProblemT103T104SourceTest,StudyProblemWorkflowTest,PdfVisualComfyT100SourceTest" test`
- `mvn -q test`

## Proximos pasos

- T108: construir capa textual PDF nativa desde `bbox-layout` para preparar lectura, busqueda y resaltado futuro.
