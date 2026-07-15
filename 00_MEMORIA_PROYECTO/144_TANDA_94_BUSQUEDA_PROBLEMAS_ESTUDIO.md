# Tanda 94 - Busqueda y filtros de problemas de estudio

## Que se implemento

- Se agrego `StudyProblemFilter` con busqueda, rango de paginas, bloque fuente y estado.
- Se agrego `StudyProblemStatusFilter` con estados: todos, sin solucion, con texto, con lienzo y con crops.
- `BuildStudyProblemsProjectionUseCase` ahora acepta filtro y conserva el metodo anterior como filtro vacio.
- `StudyProblemsProjection` reporta total de problemas para mostrar `N de M`.
- `StudyProblemListItem` expone paginas fuente, primer bloque fuente y previews de solucion/notas.
- El SideDock derecho Problema ahora tiene busqueda, filtros por capitulo/pagina/bloque/estado y contador filtrado.
- El filtro de capitulo se deriva del indice documental actual cuando hay paginas confiables.
- El panel permite elegir una fuente del problema y ejecutar `Ir a fuente`, que selecciona el bloque original en el lector.
- Los botones y filtros del panel usan `ActionButtonFactory` y `StudioFormControls`.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Cambios al esquema `.docupodcast.json`.
- Edicion de fuentes/enunciado/titulo del problema guardado.
- Galeria visual con miniaturas de crops y solucion PNG.

## Decisiones tecnicas

- El filtrado se calcula en application sobre `study.technicalProblems`; no se persiste estado de filtros.
- La navegacion a fuente reutiliza `viewModel.selectDocumentBlock(...)`; no se agrego API nueva al ViewModel.
- El filtro por capitulo usa rangos derivados de `BuildDocumentOutlineUseCase`; si no hay capitulos utiles, queda disponible el filtro por pagina/bloque.
- Los filtros de estado usan los flags ya proyectados: texto, imagen de solucion y crops fuente resueltos.

## Archivos y sistemas tocados

- `application/documentstudy`: filtro, estado, proyeccion y metadatos de lista.
- `presentation/document`: SideDock Problema con busqueda/filtros/navegacion.
- `presentation/components`: helper para text input transversal.
- `docs/productizacion`: contrato actualizado de problemas tecnicos.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildStudyProblemsProjectionUseCaseTest,StudyProblemsSearchT94SourceTest,StudyProblemsManagerT92SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 95: vista de detalle/galeria de problemas con miniaturas de crops fuente y solucion PNG.
2. Agregar apertura rapida de crop/solucion en vista ampliada sin modificar el documento fuente.
3. Evaluar seleccion multiple de problemas para exportar soluciones en lote.
