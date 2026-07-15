# Tanda 122 - Tinta fluida, diagnostico real y suavizado live

## Que se implemento

- Se agrego un modelo editable de tinta por trazos (`InkStrokeState`) con puntos crudos (`InkPointState`), tiempo, color, grosor y modo de borrador.
- El hot path de dibujo ahora solo filtra/encola puntos, remuestrea con distancia maxima de 2.5 px y pinta preview incremental.
- Se agrego una capa live separada de la tinta permanente para el trazo activo.
- Al soltar el mouse/tableta, el trazo live se confirma al modelo permanente y se convierte a comandos suavizados con segmentos de linea/curvas cuadraticas.
- El borrador en preview limpia solo tiles de tinta; no pinta con color de fondo y no toca imagenes ni fondo.
- Los cambios de color, grosor o modo de borrador fuerzan un nuevo trazo, evitando mezclar configuraciones.
- El sidecar `canvas-state.json` sube a version 2 y guarda `inkStrokes` editables, manteniendo `strokes` legacy como fallback.
- La reapertura lee primero `inkStrokes`; si no existen, conserva la lectura anterior por comandos legacy o PNG base antiguo.
- Se agregaron flags internos `INK_DIAGNOSTICS=false` y `FAST_INK_DEBUG=false` para medir cola, latencia y drops sin ensuciar la UI.

## Que quedo fuera

- No se implemento OCR nuevo.
- No se cambio el esquema principal `.docupodcast.json`.
- No se toco Domain Model Studio/UENS.
- No se agrego un hilo externo que lea eventos de mouse/tableta fuera de JavaFX; se mantuvo la politica segura de buffer rapido en FX thread y drenado por `AnimationTimer`.

## Decisiones tecnicas

- El preview puede ser raster por rendimiento, pero la exportacion y persistencia usan el modelo vectorial de trazos cuando existe.
- La capa live evita reconstruir tiles permanentes, bounds globales, sidecar o exportacion durante cada evento `MouseDragged`.
- El borrador queda limitado a la capa de tinta. Las imagenes transferidas siguen siendo contenido base sobre el que se puede escribir.
- El sidecar v2 es compatible hacia atras porque se sigue escribiendo la lista `strokes` legacy.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT122SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyDocumentUxT98SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemHotfixT116BSourceTest.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemT122SourceTest,TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,TechnicalProblemT116ESourceTest,PdfRegionTechnicalProblemT103T104SourceTest" test`
- `mvn -q "-Dtest=StudyDocumentUxT98SourceTest" test`
- `mvn -q test`

Resultado: todos pasaron. Durante `mvn -q test` aparecieron advertencias de PDFBox sobre fixtures PDF con longitudes de stream inconsistentes; no bloquearon la suite.

## Proximos pasos exactos

1. Validar manualmente escritura rapida con mouse/tableta en modo `Dibujar`, incluyendo cambio de color y grosor durante una sesion larga.
2. Guardar, cerrar y reabrir un problema con trazos nuevos para confirmar que `canvas-state.json` v2 restaura tinta e imagenes.
3. Revisar si el borrador rectangular de preview necesita mascara mas precisa por trayectoria en una tanda posterior.
4. Siguiente tanda sugerida: afinar la seleccion interna del canvas y persistencia de operaciones de copiar/mover region si el uso real lo exige.
