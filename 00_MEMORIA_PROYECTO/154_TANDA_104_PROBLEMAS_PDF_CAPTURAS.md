# Tanda 104 - Problemas tecnicos PDF por capturas

## Que se implemento

- Se agrego `StudyProblemSourceDraft` para unificar fuentes:
  - bloques de DOCX/Markdown/TXT;
  - regiones PDF con crop real y texto opcional vacio.
- `DocumentTechnicalProblemPanel` detecta PDF y cambia el flujo:
  - muestra `Seleccionar regiones PDF para asociar a problema`;
  - cuenta regiones PDF seleccionadas;
  - habilita `Generar problema` solo si hay capturas;
  - conserva checkboxes por bloque para DOCX/Markdown/TXT.
- `TechnicalProblemDialog` acepta drafts visuales con crop real mediante `showForDrafts(...)`.
- `StudyProblemWorkflow` guarda fuentes visuales PDF:
  - copia PNG temporal a `study/problems/prob-xxx/source/<source-id>.png`;
  - registra asset `STUDY_SOURCE_CROP`;
  - crea `StudySourceReference.visualRegion(...)`;
  - conserva `StudySourceReference.fullBlock(...)` para fuentes de bloque.
- `DocuPodcastShellViewModel` expone `saveTechnicalProblemFromSources(...)`.
- `DocumentWorkspaceView` abre el dialogo PDF con capturas y limpia la seleccion solo al guardar correctamente.
- Si el proyecto no tiene carpeta guardada, el flujo PDF por capturas se bloquea con mensaje accionable.

## Que quedo fuera

- Mejoras finales del modal, transferencia masiva de capturas, galeria de capturas y edicion avanzada quedan para T105/T106.
- OCR, texto nativo alineado, busqueda y resaltado quedan fuera.
- No se cambio `.docupodcast.json` v3.

## Decisiones tecnicas

- Las capturas PDF se guardan como assets semanticos ya existentes `STUDY_SOURCE_CROP`.
- `selectedText` puede quedar vacio en regiones PDF; la evidencia confiable es el crop.
- Los problemas PDF usan ids temporales estables de sesion `PDFREG-0001`, que luego quedan como `blockId` de la fuente visual.
- La ruta por bloques se mantiene para documentos no PDF.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/StudyProblemSourceDraft.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfVisualRegionCaptureUseCaseTest,PdfRegionTechnicalProblemT103T104SourceTest,PdfVisualWorkspaceT102SourceTest,PdfVisualComfyT100SourceTest,StudyProblemWorkflowTest,DocuPodcastProjectFileRepositoryTest,DocumentWorkspaceViewSourceTest" test`
- `mvn -q test`

## Proximos pasos

- T105: modal de problema tecnico definitivo, con enunciado profesional, transferencia de una/todas las capturas, pantalla completa real, Escape, split ajustable y controles no comprimidos.
