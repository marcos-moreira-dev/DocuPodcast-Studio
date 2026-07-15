# Tanda 117 - Seleccion interna del canvas y exportacion PDF

## Implementado

- El modal de problema tecnico agrega modo `Seleccionar region` en el lado del lienzo.
- La seleccion interna permite marcar un rectangulo, copiarlo como imagen compuesta, pegarlo como objeto editable, moverlo como objeto flotante y eliminar tinta/imagenes contenidas.
- La seleccion mixta de tinta + imagenes se pega como imagen compuesta v1, para mantener estabilidad sin reescribir la tinta como vectores.
- El SideDock derecho `Problema` agrega `Exportar ejercicios en PDF...`.
- La exportacion PDF usa los PNG finales ya guardados: una pagina por problema exportable, fondo blanco y proporcion preservada.
- Se genera un manifiesto junto al PDF con exportados, omitidos y errores.

## Fuera de alcance

- Edicion vectorial completa de trazos pegados.
- Exportar problemas sin PNG final.
- Cambios de persistencia.
- OCR o busqueda PDF.

## Decisiones

- La operacion `Eliminar seleccion` borra solo tinta dentro del rectangulo y elimina imagenes completamente contenidas. No recorta parcialmente imagenes.
- `Copiar/Pegar` produce un objeto imagen editable para reducir riesgo de inconsistencias del motor de tinta.
- El PDF masivo trabaja con soluciones finales guardadas, no con snapshots vivos de modales abiertos.

## Archivos tocados

- `TechnicalProblemDialog`
- `StudyProblemCanvasSurface`
- `DocumentTechnicalProblemPanel`
- `DocuPodcastShellViewModel`
- `StudyProblemWorkflow`
- `study-problem.css`
- `ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,PdfVisualFlowT113SourceTest,StudyProblemWorkflowTest" test`
- `mvn -q "-Dtest=TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,PdfVisualFlowT113SourceTest,StudyProblemWorkflowTest,PdfEmbeddedRendererT99SourceTest,StudyDocumentUxT97SourceTest,StudyDocumentUxT98SourceTest" test`
- `mvn -q test`

## Siguiente tarea exacta

- Probar manualmente un problema con imagen + tinta: seleccionar region, copiar, pegar, mover y exportar PNG/PDF. Luego revisar si hace falta una accion de `Duplicar seleccion` como atajo.
