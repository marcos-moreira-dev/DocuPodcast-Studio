# Hotfix T115B/T116 - Lienzo, captura PDF y exportacion de problemas

## Implementado

- `TechnicalProblemDialog` mantiene las herramientas del lienzo visibles en pantalla completa y permite cargar imagenes externas desde disco.
- Las imagenes externas se agregan como fuentes visuales del problema y se pueden transferir al lienzo igual que los crops o imagenes del documento.
- `StudyProblemCanvasSurface` exporta desde capas internas: fondo, titulo quemado, imagenes y tinta. Ya no depende de snapshots grandes de nodos JavaFX para componer el PNG final.
- El lienzo conserva tiles acotados y copia sus rasters internos para reducir riesgo de errores `NGCanvas/RTTexture`.
- El modo `Panear` desactiva temporalmente `Interactuar con imagenes`.
- La captura PDF usa un overlay propio sobre la imagen renderizada para calcular el rectangulo visible y convertirlo a captura.
- El toggle `Desplazar playbar` muestra controles de reproduccion en el rail izquierdo incluso cuando el SideDock derecho no esta abierto.
- El SideDock Problema agrega `Renderizar y exportar todos...`, que copia PNGs finales existentes a una carpeta elegida y escribe `manifest.txt`.

## Fuera de alcance

- OCR nuevo o reconstruccion LaTeX.
- Marcadores persistentes de pagina PDF.
- Persistir objetos editables de imagenes del lienzo en JSON; las imagenes se siguen fusionando en el PNG final.
- Cambios en Domain Model Studio/UENS.

## Decisiones tecnicas

- Se agrego `ProjectAssetKind.STUDY_PROBLEM_IMAGE` para identificar imagenes externas de problemas sin cambiar la estructura del `.docupodcast.json`.
- Las imagenes externas reutilizan `StudySourceReference.sourceCropAssetId` como referencia visual de fuente.
- La exportacion masiva trabaja solo con problemas guardados que ya tienen `STUDY_SOLUTION_IMAGE`; los demas se omiten y se reportan en el manifiesto.
- Para exportar imagenes del lienzo, se calculan bounds logicos desde `ImageView.layoutX/layoutY` y `fitWidth/fitHeight`, evitando depender de bounds visuales no actualizados.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assets/ProjectAssetKind.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assets/ProjectAssetReference.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/StudyProblemSourceDraft.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=StudyProblemWorkflowTest,PdfRegionTechnicalProblemT103T104SourceTest,StudyDocumentUxT97SourceTest,StudyProblemExportT107SourceTest,StudyDocumentUxT98SourceTest" test`
- `mvn -q "-Dtest=PdfVisualFlowT113SourceTest,TheatrePlaybarLayoutSourceTest" test`
- `mvn -q test`

## Proximos pasos

1. Validar manualmente captura rectangular PDF con el libro de matematicas real.
2. Implementar marcadores persistentes de pagina PDF: crear, nombrar, ir y eliminar.
3. Revisar exportacion de problemas reabiertos para decidir si se persisten objetos editables de imagenes en una version futura del contrato.
