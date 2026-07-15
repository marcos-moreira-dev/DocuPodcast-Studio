# Tanda 125-126 - Tinta fluida transversal y auditoria del modal tecnico

Fecha: 2026-07-02

## Que se implemento

- Se creo `presentation.ink.InkRealtimeStrokeEngine` como motor reusable de baja latencia para captura y render live de tinta.
- El handler JavaFX del modal de problema tecnico ahora delega en el motor:
  - `begin(...)`, `move(...)`, `end(...)` solo encolan puntos crudos;
  - un `AnimationTimer` drena el buffer con presupuesto de 4 ms por frame;
  - el preview usa segmentos remuestreados a 2.5 px y curvas cuadraticas por midpoint;
  - el commit genera trazos vectoriales para `StudyProblemCanvasSurface`.
- Se retiro del flujo caliente del modal la cola inline vieja basada en `pendingStrokeSamples` y `InkStrokePipeline.resampleLine`.
- Se agregaron flags apagados por defecto:
  - `INK_PERF_DIAGNOSTICS = false`;
  - `FAST_INK_DEBUG = false`;
  - `PDF_RENDER_DIAGNOSTICS = false`.
- Se agrego diagnostico opcional del render PDF alrededor de cola, inicio, exito, error y limpieza de renderizados.
- Se actualizaron guardarrailes de source test para validar la nueva frontera transversal.

## Decisiones tecnicas

- No se leera input desde threads externos a JavaFX. La optimizacion queda en buffer rapido + `AnimationTimer`, manteniendo seguridad de JavaFX.
- El render PDF queda aislado por su cola actual; la tinta no dispara render PDF ni espera por el visor durante `drag`.
- El motor se ubica en `presentation.ink` porque su primera responsabilidad es superficie JavaFX/input/render live. El estado editable generico sigue en `application.ink`.
- T126 se cierra como base de desacople: el hot path salio del modal, pero imagenes, seleccion, exportacion y serializacion aun pueden extraerse mas adelante a controladores reutilizables.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngineSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT122SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT124InkArchitectureSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT116ESourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualFlowT113SourceTest.java`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemT122SourceTest,TechnicalProblemT124InkArchitectureSourceTest,InkRealtimeStrokeEngineSourceTest,PdfVisualFlowT113SourceTest" test`
- `mvn -q "-Dtest=TechnicalProblemT116ESourceTest" test`
- `mvn -q test`

Resultado: verde. El suite completo solo emitio warnings de PDFBox sobre offsets de streams en fixtures PDF.

## Que quedo fuera

- Integracion real de libreria stylus nativa/presion de lapiz.
- Extraccion completa de imagenes, recorte, seleccion, exportacion y sidecar desde `StudyProblemCanvasSurface` hacia controladores `presentation.ink`.
- Bocetos de teatro o consumidor `TheatreFrameSketch`.
- OCR, indice PDF y cambios en `.docupodcast.json`.

## Proximos pasos exactos

1. T127: validar visualmente con tableta fisica el nuevo motor de tinta: trazo rapido de 10 segundos, cambios de color/grosor, borrador sobre imagen y exportacion HD.
2. Si persiste latencia, activar temporalmente `INK_PERF_DIAGNOSTICS` y medir `queued`, `pending`, `peak` y `dropped` mientras se dibuja.
3. Extraer `InkImageLayerController`, `InkSelectionController` e `InkWorkspaceExporter` desde `StudyProblemCanvasSurface` hacia `presentation.ink`.
4. Preparar el punto de integracion futuro de teatro para bocetos de frames usando `InkWorkspaceState`, sin acoplar teatro a estudio documental.
