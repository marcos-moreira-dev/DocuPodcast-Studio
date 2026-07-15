# Tandas 120-121 - Recorte real, escritura fluida y estado editable

## Que se implemento

- Se habilito `Recortar imagen` cuando hay una imagen seleccionada y el modo `Interactuar con imagenes` esta activo.
- Se agrego modo de recorte sobre la imagen seleccionada: el usuario arrastra un rectangulo dentro de la imagen y se crea una version derivada para el problema actual.
- Se agrego `Restaurar recorte` para volver a la imagen original cuando existe respaldo.
- La exportacion PNG ahora usa bounds reales del contenido: titulo, imagenes y trazos, con margen util de 100 px a derecha y abajo.
- La exportacion mantiene escala HD, intentando 4x y bajando de forma controlada si excede el limite seguro de pixeles.
- Se reemplazaron mensajes truncados en flujos de problema tecnico por contenido con wrap y ancho suficiente.
- La entrada del lapiz ahora usa buffer rapido de puntos en JavaFX y drenado por lotes con `AnimationTimer`.
- Los trazos se guardan como comandos/estado vectorial suficiente para reexportar con mas calidad y no depender solo del bitmap de preview.
- Cambios de color, grosor o borrador empiezan un trazo nuevo para evitar mezclar configuraciones.
- Se agrego estado editable sidecar por problema en `study/problems/<problem-id>/solution/canvas-state.json`.
- Al abrir un problema, si existe sidecar, se restauran fondo, imagenes, recortes, posiciones y trazos. Si no existe, se usa el PNG viejo como capa base dibujable.
- `Guardar`, `Guardar cambios` y guardado desde creacion de problema persisten PNG compuesto y sidecar editable cuando corresponde.

## Que quedo fuera

- No se implemento OCR nuevo.
- No se cambio el esquema principal `.docupodcast.json`.
- No se toco Domain Model Studio/UENS.
- No se rehizo todavia la arquitectura completa de seleccion interna mixta del lienzo como objetos vectoriales separados; la seleccion avanzada queda para una tanda posterior si se necesita mas precision.

## Decisiones tecnicas

- El recorte no modifica la fuente original: cada crop visible del lienzo guarda respaldo interno y solo afecta la imagen del problema actual.
- El estado editable vive como sidecar auxiliar del proyecto para no romper compatibilidad de proyectos existentes.
- La exportacion calcula el rectangulo minimo necesario desde contenido real y no desde el tamano total del lienzo, evitando PNGs enormes vacios.
- El borrador sigue siendo borrador de tinta: no altera imagenes ni fondo.
- El render por lotes mantiene todo acceso a nodos JavaFX en el hilo JavaFX; no se agrego un thread externo que toque UI.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasExportOptions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemDetail.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/BuildStudyProblemsProjectionUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test`

Resultado: ambos pasaron. Durante `mvn -q test` aparecieron advertencias de PDFBox sobre fixtures PDF con longitudes de stream inconsistentes, pero no bloquearon la suite.

## Proximos pasos exactos

1. Ejecutar `mvn -q test` y corregir cualquier fallo real.
2. Validar manualmente: seleccionar imagen, recortar, restaurar, dibujar rapido, guardar, cerrar y reabrir problema.
3. Validar que `canvas-state.json` aparezca bajo `study/problems/<problem-id>/solution/` en problemas guardados.
4. Siguiente tanda sugerida: estabilizar seleccion interna avanzada del canvas y mejorar herramientas de copiar/mover region si el uso real lo exige.
