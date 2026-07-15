# Tanda 90 - Recortes PDF fieles para estudio documental

## Que se implemento

- El importador PDF intenta primero `pdftotext -bbox-layout`.
- Se agrego parser XHTML seguro para la salida bbox-layout de Poppler.
- Los bloques PDF nativos pueden conservar `sourcePage`, `bbox`, `bboxUnits`, `pageWidth`, `pageHeight`, `extractionMode` y `confidence`.
- Se mantiene fallback a `pdftotext -layout`, y luego al extractor Java si Poppler falla o no produce texto suficiente.
- Se agrego el puerto `SourceCropRenderer` y el caso de uso `PrepareStudySourceCropsUseCase`.
- Se agrego `PdfSourceCropRenderer`, que usa `pdftoppm` mediante `DefaultExternalProcessRunner`, renderiza pagina completa y recorta con `ImageIO`.
- El modal de problema tecnico muestra crops fuente junto al texto seleccionado cuando existen.
- Al guardar un problema, los crops temporales se copian a `study/problems/<problem-id>/source/<block-id>.png`.
- Cada crop guardado se registra como asset `STUDY_SOURCE_CROP`.
- Cada `StudySourceReference` guarda el `sourceCropAssetId` correspondiente.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Indice documental robusto para libros grandes.
- Gestor/listado de problemas guardados.
- Presion de lapiz de tableta digitalizadora.

## Decisiones tecnicas

- Las formulas dificiles se preservan primero como recorte fiel del PDF, no como LaTeX reconstruido.
- Los crops no son obligatorios: si `bbox` o `pdftoppm` fallan, el flujo continua con texto.
- La UI no importa infraestructura directamente; presentation llama un caso de uso de application y la factory inyecta el renderer Poppler.
- No se cambio `formatVersion`: se reutilizo el contrato v3 existente con `sourceCropAssetId`.
- No se agrego `ProcessBuilder` directo fuera del runner comun.

## Archivos y sistemas tocados

- PDF: `PdfDocumentImporter`, `PdfBboxLayoutParser`, `PdfBox`, `PdfSourceCropRenderer`.
- Application: `SourceCropRenderer`, `PrepareStudySourceCropsUseCase`, `DocumentApplicationServices`.
- Bootstrap: `ApplicationServicesFactory`.
- Estudio: `StudySourceReference`, `StudyProblemWorkflow`.
- UI documental: `DocumentWorkspaceView`, `TechnicalProblemDialog`, `study-problem.css`.
- Docs: `DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`, `ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`.
- Tests: parser bbox, renderer crop, importador PDF, workflow de problema, source tests UI y arquitectura.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfBboxLayoutParserTest,PdfSourceCropRendererTest,PdfDocumentImporterTest,StudyProblemWorkflowTest,DocumentWorkspaceViewSourceTest,ExternalProcessRunnerRf1SourceTest" test`
- `mvn -q "-Dtest=ArchitectureBoundaryTest,PdfBboxLayoutParserTest,PdfSourceCropRendererTest,PdfDocumentImporterTest,StudyProblemWorkflowTest,DocumentWorkspaceViewSourceTest,ExternalProcessRunnerRf1SourceTest" test`
- `mvn -q test`
- `mvn -q "-Dtest=ProjectMemoryDocumentationTest" test`

Nota: una ejecucion intermedia de `mvn -q test` fallo porque `presentation` importaba `infrastructure` desde el helper de previews. Se corrigio moviendo la preparacion de crops a application y la suite final quedo verde.

## Proximos pasos exactos

1. Tanda 91: mejorar indice documental robusto para libros grandes.
2. Detectar bookmarks PDF cuando existan y mapearlos a bloques/paginas.
3. Detectar paginas tipo Contents/Table of Contents y entradas con numero de pagina.
4. Detectar capitulos/secciones numeradas y usarlas para arbol documental.
5. Mantener fallback plano por bloques cuando no haya estructura confiable.
