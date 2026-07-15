# Tanda 89 - Estudio documental: problemas tecnicos

## Que se implemento

- Se agrego la capa `study` al proyecto `.docupodcast.json` y el formato subio a `formatVersion = 3`.
- Se agregaron `StudyProjectLayer`, `TechnicalProblem` y `StudySourceReference`.
- Se agregaron assets semanticos `STUDY_SOURCE_CROP` y `STUDY_SOLUTION_IMAGE`.
- Se agrego el comando `PREPARE_TECHNICAL_PROBLEM` al catalogo, disponibilidad, menu y ribbon de Estudio.
- El lector documental puede activar checkboxes por bloque para armar el enunciado.
- El SideDock incorpora el modulo `Problema` con contador, preview, limpiar seleccion y generar problema.
- Se agrego un modal de resolucion con enunciado a la izquierda y solucion a la derecha en modo Texto o Lienzo.
- El lienzo soporta lapiz, grosor, color, fondo, borrador, deshacer, rehacer, limpiar, scroll vertical y exportacion PNG al guardar.
- El PDF importer ahora conserva `sourcePage`, `bbox`, `extractionMode` y `confidence` en bloques nativos.
- El PDF importer intenta `pdftotext -layout` mediante `DefaultExternalProcessRunner` y mantiene fallback Java.
- PDFs escaneados o sin texto suficiente abren en fallback visual con issues `pdf-visual-only` y `pdf-ocr-pending`, sin OCR.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Crops reales por bbox desde `pdftoppm`.
- Deteccion robusta de indices para libros grandes mas alla de heuristicas basicas por encabezados/secciones.
- Presion de lapiz de tableta digitalizadora.
- Gestion avanzada de multiples soluciones o historial de soluciones por problema.

## Decisiones tecnicas

- El documento fuente sigue siendo solo lectura; problemas, soluciones y assets viven solo en el proyecto.
- Para formulas dificiles se prepara el contrato de crops (`sourcePage`, `bbox`, `STUDY_SOURCE_CROP`), pero no se fuerza LaTeX.
- La exportacion del lienzo se guarda como PNG bajo `study/problems/<problem-id>/solution.png`.
- La logica de guardado de problemas vive en `StudyProblemWorkflow` para no aumentar deuda del `DocuPodcastShellViewModel`.
- `pdftotext` se ejecuta por el runner comun de procesos, no por `ProcessBuilder` directo.

## Archivos y sistemas tocados

- Dominio: `domain/study`, `DocuPodcastProject`, `ProjectAssetKind`, `ProjectAssetReference`.
- Persistencia: `DocuPodcastProjectJsonReader`, `DocuPodcastProjectJsonWriter`, `DocuPodcastProjectFormat`.
- PDF: `PdfDocumentImporter`.
- UI comandos: `AppCommandId`, `AppCommandRegistry`, `CommandAvailabilityPolicy`, `RibbonDefinitionCatalog`, `RibbonIconCatalog`, `DocuPodcastShellView`.
- UI lector: `DocumentWorkspaceView`, `DocumentTechnicalProblemPanel`, `TechnicalProblemDialog`, `SideDockModuleId`.
- Workflow: `StudyProblemWorkflow`.
- CSS: `document/study-problem.css`, importado desde `docupodcast-light.css`.
- Docs: `DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`, `ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`.
- Tests: JSON v3/study, PDF fallback visual, ribbon/comandos, source-tests y smoke de cerebro.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=DocuPodcastProjectFileRepositoryTest,PdfDocumentImporterTest,TextAnchorMigrationT93SourceTest,DocumentWorkspaceViewSourceTest,AppCommandRegistryTest,RibbonCommandContractTanda8Test" test`
- `mvn -q test`

## Proximos pasos exactos

1. Implementar crops reales de PDF con Poppler `pdftoppm` por `sourcePage` y `bbox`, guardando assets `STUDY_SOURCE_CROP`.
2. Mejorar indice documental: bookmarks, paginas Contents, capitulos/secciones numeradas y entradas con numero de pagina.
3. Mostrar crops fuente dentro del modal de problema junto al texto seleccionado.
4. Agregar lista/gestor de problemas guardados en el SideDock para reabrir, editar o exportar soluciones.
5. Evaluar OCR local opcional para PDFs visuales, manteniendolo desactivado por defecto y claramente diagnosticado.
