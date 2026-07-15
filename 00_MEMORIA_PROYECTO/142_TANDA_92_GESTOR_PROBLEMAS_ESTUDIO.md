# Tanda 92 - Gestor de problemas guardados en SideDock

## Que se implemento

- El panel SideDock `Problema` ahora tiene dos zonas: nuevo problema y problemas guardados.
- Se agrego una proyeccion de problemas guardados en `application.documentstudy`.
- La lista se ordena por `updatedAt` descendente y muestra badges de fuentes, crops, texto y lienzo.
- El detalle resuelve rutas seguras de crops fuente y solucion PNG dentro de la carpeta del proyecto.
- Se agregaron operaciones en `StudyProblemWorkflow` para actualizar solucion/notas, eliminar problema y exportar PNG/TXT.
- `TechnicalProblemDialog` ahora tiene modo edicion para reabrir un problema guardado con enunciado solo lectura.

## Que quedo fuera

- Edicion de titulo, fuentes, crops o enunciado.
- Nuevo formato JSON.
- OCR, LaTeX editable o cambio de deteccion matematica.
- Busqueda/filtros por pagina, capitulo o bloque fuente.

## Decisiones tecnicas

- No se creo un SideDock nuevo: el gestor vive dentro del modulo `DOCUMENT_TECHNICAL_PROBLEM`.
- El ViewModel solo expone delegaciones compactas; la logica de assets queda en workflow/proyeccion.
- Al editar, un lienzo nuevo solo reemplaza/crea `STUDY_SOLUTION_IMAGE` si el usuario dibujo algo.
- Al eliminar, se quitan assets `STUDY_SOURCE_CROP` y `STUDY_SOLUTION_IMAGE`; los archivos se borran solo si resuelven de forma segura dentro del proyecto.
- Exportar PNG requiere que el asset exista y que el proyecto tenga carpeta guardada; exportar TXT solo requiere solucion textual.

## Archivos y sistemas tocados

- `application/documentstudy`: proyeccion de problemas guardados.
- `presentation/shell/workflow/StudyProblemWorkflow.java`
- `presentation/document/DocumentTechnicalProblemPanel.java`
- `presentation/document/TechnicalProblemDialog.java`
- `domain/study/TechnicalProblem.java`
- `application/services/DocumentStudyApplicationServices.java`
- `bootstrap/ApplicationServicesFactory.java`
- Tests de proyeccion, workflow y source UI.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildStudyProblemsProjectionUseCaseTest,StudyProblemWorkflowTest,StudyProblemsManagerT92SourceTest,DocumentWorkspaceViewSourceTest" test`
- `mvn -q "-Dtest=ProjectMemoryDocumentationTest,StudyProblemsManagerT92SourceTest,BuildStudyProblemsProjectionUseCaseTest,StudyProblemWorkflowTest,ArchitectureBoundaryTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 93: filtros, busqueda y navegacion rapida entre problemas guardados por capitulo, pagina o bloque fuente.
2. Agregar accion para saltar desde un problema guardado al primer bloque fuente del lector.
3. Mejorar agrupacion visual por pagina/capitulo cuando el indice documental robusto pueda aportar contexto.
