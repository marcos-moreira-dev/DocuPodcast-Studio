# Plan T123B/T124 - Arquitectura de entrada de stylus, proveedor nativo y motor de tinta

Este documento registra la planificacion y cierre tecnico para corregir la escritura lenta y angular del lienzo tecnico. La decision principal es separar entrada, pipeline, render y persistencia antes de integrar una libreria nativa de stylus.

## Estado De Implementacion

- 2026-07-01: T123B implementada. Se agrego la frontera `InkInputProvider` con fallback `JavaFxMouseInputProvider`, muestras normalizadas `InkInputSample` y conexion desde `TechnicalProblemDialog`.
- El modal ya no registra handlers directos `MOUSE_PRESSED`, `MOUSE_DRAGGED` ni `MOUSE_RELEASED` para la tinta del lienzo; los eventos se consumen mediante el proveedor de entrada.
- `InkInputSample` ya reserva presion y cursor borrador para una futura integracion nativa.
- 2026-07-01: T124 implementada. Se creo la base transversal `application.ink` para modelo puro, `presentation.ink` para entrada/pipeline reusable y un sidecar generico v3 `InkWorkspaceState`.
- `TechnicalProblemDialog` queda como primer consumidor: serializa/restaura el estado de tinta mediante el motor transversal, pero no cambia `.docupodcast.json`.
- `LectureStudioStylusInputProvider` queda aislado detras de `InkInputProviderFactory`; detecta capacidad por reflexion y cae a `JavaFxMouseInputProvider` si la libreria/nativos no estan disponibles.
- No se agrego una dependencia dura a `lectureStudio/stylus`: la presion real y afinado de latencia quedan para T125 despues de validar la libreria con tableta real.
- Queda un hook documental de teatro (`TheatreFrameSketchInkHook`) para que una tanda futura pueda guardar bocetos de frames con `InkWorkspaceState` sin depender de estudio documental.
- 2026-07-02: T131/T132 implementadas sobre la base transversal. El modo `Dibujar` captura tinta por encima de imagenes colocadas, y la presion por punto se conserva en el sidecar y solo modula grosor cuando el proveedor activo es nativo real.

## Resumen Ejecutivo

- El problema visible no debe atacarse metiendo una dependencia nativa directamente en `StudyProblemCanvasSurface`.
- Primero se debe crear una frontera de entrada: `InkInputProvider`.
- `JavaFxMouseInputProvider` debe encapsular el flujo actual de `MouseEvent` y quedar como fallback permanente.
- `lectureStudio/stylus` es viable como experimento posterior, porque expone presion, cursor `PEN/ERASER/MOUSE`, tilt, rotation y tangent pressure, pero carga una libreria nativa por JNI y puede fallar por plataforma/empaquetado.
- T123B debe desacoplar y diagnosticar. T124 debe integrar `lectureStudio/stylus` solo detras de adapter + factory + fallback.
- No se cambia `.docupodcast.json`.
- No se toca Domain Model Studio/UENS.
- No se rompe `canvas-state.json` v2 ni problemas viejos.

## Diagnostico Del Estado Actual

### Entrada

- `TechnicalProblemDialog` todavia importa `javafx.scene.input.MouseEvent`.
- `initializeCanvas()` conecta directamente `MOUSE_MOVED`, `MOUSE_PRESSED`, `MOUSE_DRAGGED` y `MOUSE_RELEASED` sobre `drawingSurface`.
- La conversion de coordenadas se hace con `drawingSurface.sceneToLocal(...)`.
- No existe una interfaz `InkInputProvider`.
- No existe un proveedor separado para mouse/tableta.
- La presion no existe en el modelo actual. En la practica todo funciona como `pressure = 1.0`.
- El borrador usa el mismo flujo que el lapiz mediante `eraser.isSelected()`.

### Pipeline De Tinta

- `TechnicalProblemDialog` encola `StrokeSample` en `pendingStrokeSamples`.
- `AnimationTimer` drena esa cola con `flushPendingStrokeSamples(STROKE_DRAIN_LIMIT)`.
- El hot path actual hace demasiado trabajo cerca del drag:
  - filtra distancia;
  - interpola puntos;
  - crea `StrokeSample`;
  - empieza/termina strokes;
  - pinta preview;
  - construye `InkPointState`;
  - decide splits por color/grosor/borrador.
- `StudyProblemCanvasSurface` ya tiene un modelo por tiles y separa:
  - background tiles;
  - stroke tiles;
  - live stroke tiles;
  - image layer.
- Tambien guarda `InkStrokeState`, `InkPointState` y comandos heredados.
- El sidecar actual usa `canvas-state.json` version 2 con `inkStrokes`, `strokes` e `images`.

### Render Y Persistencia

- `StudyProblemCanvasSurface` sabe exportar con imagenes, fondo e ink.
- `TechnicalProblemDialog` todavia coordina demasiado:
  - entrada;
  - render live;
  - estado de UI;
  - sidecar JSON;
  - guardado;
  - exportacion;
  - restauracion.
- La exportacion ya intenta usar modelo vectorial, pero la entrada pobre limita la calidad final.

## Riesgos Detectados

- JavaFX puede coalescer eventos de mouse/tableta si la UI esta cargada. Si llegan pocos eventos reales, el suavizado solo puede maquillar lineas largas.
- Integrar una libreria nativa sin adapter puede hacer que la app no arranque cuando falle JNI.
- El modulo base de `lectureStudio/stylus` se llama `stylus`, no un nombre largo estable de proyecto; eso afecta `module-info.java`.
- `StylusManager` carga la libreria nativa en bloque `static`. Un error nativo debe aislarse para no tumbar el flujo principal.
- Los samples de presion pueden venir fuera de rango, `NaN`, `0`, o no estar disponibles segun hardware/driver.
- Un proveedor nativo puede entregar coordenadas con referencia distinta a JavaFX; por eso hace falta `InkCoordinateMapper`.
- Con `-Xmx2048m`, la densidad alta de muestras puede saturar memoria si no hay limites.
- Si se persiste presion/tilt sin compatibilidad, se pueden romper problemas viejos.

## Decision Sobre lectureStudio/stylus

Decision recomendada:

1. No usar `lectureStudio/stylus` en T123B.
2. Preparar adapter y factory primero.
3. Integrar `lectureStudio/stylus` en T124 como proveedor experimental, no como dependencia acoplada al lienzo.
4. Mantener `JavaFxMouseInputProvider` como fallback siempre.
5. Si la libreria nativa falla, la app debe reportar el fallback y seguir funcionando.

Motivo:

- La libreria existe y es compatible con el objetivo: `StylusEvent` expone axes, `StylusCursor` distingue `PEN`, `ERASER` y `MOUSE`, y `StylusAxesData` expone `pressure`, `tangentPressure`, `tiltX`, `tiltY` y `rotation`.
- Tambien hay riesgo real: `StylusManager` carga una libreria nativa llamada `stylus`.
- Por tanto, la integracion debe vivir detras de `LectureStudioStylusInputProvider`, no dentro de `StudyProblemCanvasSurface`.

## Arquitectura Propuesta

### Flujo Textual

```text
Stylus / Mouse
  -> InkInputProvider
  -> InkInputSample
  -> InkInputListener
  -> InkPipeline
  -> InkRenderer live
  -> InkStrokeState
  -> canvas-state.json
  -> exportacion PNG
```

### Responsabilidades

`InkInputProvider`

- Fuente de entrada.
- Adjunta/desadjunta listeners sobre un `Node`.
- No renderiza.
- No guarda.
- No conoce sidecar.
- No exporta.

`InkInputSample`

- Muestra semantica independiente de JavaFX y de la libreria nativa.
- Debe representar una muestra real, no un punto ya remuestreado.

Campos sugeridos:

```java
double x;
double y;
double pressure;
double tangentPressure;
double tiltX;
double tiltY;
double rotation;
long timeNanos;
boolean eraser;
String source;
int pointerId;
boolean primaryButton;
boolean barrelButton;
```

`InkInputListener`

- Recibe `onInkPressed`, `onInkMoved`, `onInkReleased`, `onInkCancelled`.
- No debe recibir `MouseEvent` ni `StylusEvent` directamente.

`InkInputProviderFactory`

- Selecciona proveedor activo.
- Orden:
  1. `LectureStudioStylusInputProvider`, si esta habilitado y carga bien.
  2. `JavaFxMouseInputProvider`, siempre disponible.
- Reporta nombre de proveedor, capacidades y razon de fallback.

`InkPipeline`

- Normaliza y estabiliza muestras.
- Separa:
  - `rawSamples`;
  - `resampledSamples`;
  - `stabilizedSamples`;
  - `smoothedSegments`;
  - `InkStrokeState`.
- Debe iniciar un nuevo trazo cuando cambien color, grosor, herramienta o proveedor.

`InkRenderer`

- Render live barato.
- Render permanente.
- Render para exportacion HD.
- Render de borrador.
- No debe tomar decisiones de proveedor.

`InkDiagnostics`

- Mide una vez por segundo:
  - `providerName`;
  - `rawSamples/sec`;
  - `maxRawDistance`;
  - `avgRawDistance`;
  - `maxTimeBetweenSamplesMs`;
  - `avgTimeBetweenSamplesMs`;
  - `pressureMin`;
  - `pressureMax`;
  - `eraserEvents/sec`;
  - `interpolatedSamples/sec`;
  - `renderPreviewMaxMs`;
  - `droppedSamples`.

`InkCoordinateMapper`

- Centraliza coordenadas:
  - pantalla;
  - escena;
  - node;
  - coordenada logica del lienzo;
  - zoom;
  - pan/scroll;
  - escala de exportacion.

## Paquetes Propuestos

```text
com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input
  InkInputProvider
  InkInputListener
  InkInputSample
  InkInputCapabilities
  InkPointerKind
  InkInputProviderFactory
  JavaFxMouseInputProvider
  LectureStudioStylusInputProvider   (T124)

com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.model
  InkStroke
  InkPoint
  InkTool
  InkStrokeBuilder

com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.pipeline
  InkPipeline
  InkPipelineConfig
  InkResampler
  InkSmoother

com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.render
  InkRenderer
  JavaFxTileInkRenderer

com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.diagnostics
  InkDiagnostics
  InkDiagnosticsSnapshot

com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.geometry
  InkCoordinateMapper
```

Si se quiere mantener scope bajo en T123B, `render` y `model` pueden empezar package-private y solo promoverse cuando el codigo deje de caber sanamente en `TechnicalProblemDialog`.

## Clases Existentes A Modificar

`TechnicalProblemDialog`

- Quitar los handlers directos de `MouseEvent` del flujo de tinta.
- Mantener listeners de UI, imagenes, botones y modos.
- Crear `InkInputProviderFactory`.
- Adjuntar proveedor a `drawingSurface`.
- Pasar samples a `InkPipeline`.
- Mantener `Guardar`, `Guardar cambios`, sidecar y exportacion.

`StudyProblemCanvasSurface`

- Debe quedarse como superficie visual y exportadora.
- Debe exponer metodos claros para:
  - iniciar live stroke;
  - preview line/quadratic;
  - commit stroke;
  - erase stroke/region;
  - restore/export.
- No debe importar `MouseEvent`.
- No debe importar `lectureStudio/stylus`.

`module-info.java`

- T123B: sin cambios de dependencia nativa.
- T124: agregar `requires stylus;` solo despues de confirmar modulo real con `jar --describe-module`.
- Si se aisla en profile/reflection, documentar esa decision.

`pom.xml`

- T123B: sin cambios.
- T124: agregar dependencia experimental, preferiblemente bajo profile o flag:
  - `org.lecturestudio.stylus:stylus-javafx:0.3.0`
  - posiblemente `org.lecturestudio.stylus:stylus:0.3.0`, si Maven no lo trae transitivamente.

Tests fuente

- Agregar tests que verifiquen que el flujo de tinta ya no se cablea directamente con `drawingSurface.addEventHandler(MouseEvent...)`.
- Agregar tests de fallback del provider.
- Agregar tests de sidecar compatible.

## Que No Se Debe Tocar

- `.docupodcast.json`.
- Domain Model Studio/UENS.
- Persistencia principal v3.
- Flujo PDF de capturas.
- OCR.
- ComfyUI.
- Voces.
- Export Center.
- El contrato existente de `STUDY_SOURCE_CROP`, `STUDY_PROBLEM_IMAGE` y `STUDY_SOLUTION_IMAGE`.
- Problemas antiguos sin sidecar: deben seguir cargando PNG base dibujable.

## Plan Por Fases

### T123B / Fase 1 - Diagnostico Y Desacoplamiento Minimo

Objetivo: medir y separar entrada sin cambiar comportamiento visible.

Cambios:

- Crear `InkInputProvider`, `InkInputListener`, `InkInputSample`, `InkInputCapabilities`.
- Crear `JavaFxMouseInputProvider`.
- Mover `MouseEvent` de tinta desde `TechnicalProblemDialog` al proveedor JavaFX.
- Crear `InkInputProviderFactory` con un solo proveedor real por ahora: JavaFX.
- Mantener `AnimationTimer`, pero renombrar su rol mental: renderer/drainer, no sampler.
- Agregar `InkDiagnostics` con metricas reales:
  - muestras crudas por segundo;
  - distancia maxima entre muestras reales;
  - tiempo maximo entre muestras;
  - puntos interpolados;
  - drops.
- Confirmar que el sidecar v2 sigue guardando igual.

Resultado esperado:

- Misma UI.
- Misma exportacion.
- Mejor diagnostico.
- Cero dependencia nativa.
- Base lista para T124.

### T124 / Fase 2 - Proveedor Experimental lectureStudio/stylus

Objetivo: probar si el cuello de botella era JavaFX MouseEvent.

Cambios:

- Agregar dependency/profile experimental.
- Crear `LectureStudioStylusInputProvider`.
- Mapear:
  - `StylusEvent.getAxesData().getX()/getY()`;
  - `getPressure()`;
  - `getTangentPressure()`;
  - `getTiltX()/getTiltY()`;
  - `getRotation()`;
  - `StylusCursor.ERASER`.
- Usar `StylusListener` para button down/up/move.
- Si el provider no carga, fallback automatico a JavaFX.
- Mostrar en diagnostico:
  - proveedor activo;
  - soporte de presion;
  - rango de presion visto;
  - razon de fallback.
- No tocar recorte, exportacion ni OCR.

Resultado esperado:

- Se puede comparar JavaFX vs stylus nativo.
- La app sigue funcionando si la libreria falla.

### Fase 3 - Presion Y Borrador Real

Objetivo: aprovechar datos nativos solo cuando existan.

Cambios:

- Normalizar presion:
  - `NaN`, infinito o `<= 0` -> `1.0`;
  - valores validos -> clamp `0.0..1.0`;
  - `width = baseWidth * pressureCurve(pressure)`.
- Curva inicial:
  - minimo de grosor visual para que no desaparezca;
  - curva suave, no lineal agresiva.
- Si cursor nativo es `ERASER`, usar borrador automatico mientras dure ese cursor.
- El borrador sigue afectando solo tinta.
- Preparar sidecar tolerante:
  - si se agregan `pressure`, `tiltX`, `tiltY`, `rotation`, el lector debe tolerar ausencia.
  - No cambiar `.docupodcast.json`.

### Fase 4 - Refinamiento De Render

Objetivo: que presion y muestras densas se vean fluidas.

Cambios:

- Live preview con grosor variable si la presion es confiable.
- Render permanente desde el mismo modelo.
- Exportacion HD con replay vectorial.
- Reducir trabajo dentro del handler de entrada.
- Mantener batching por `AnimationTimer`.

### Fase 5 - Limpieza, Tests Y Docs

Objetivo: cerrar la arquitectura.

Cambios:

- Tests de arquitectura.
- Tests de fallback.
- Tests de sidecar v2/v3 auxiliar si existe.
- Documentar diagnostico de stylus.
- Documentar como desactivar provider nativo.

## Manejo De Presion

Reglas propuestas:

- `pressure <= 0`, `NaN`, infinito o ausente: `1.0`.
- Si el proveedor reporta presion constante `0` o `1`, marcar `supportsPressure=false` en diagnostico efectivo.
- `pressure` se guarda por punto solo cuando el provider lo soporta de forma util.
- La UI del grosor sigue siendo base width; presion modula ese valor.
- El borrador puede tener curva separada para no borrar de forma irregular.

## Manejo De Borrador

- Modo manual actual sigue existiendo.
- Si provider nativo reporta `StylusCursor.ERASER`, el input sample llega con `eraser=true`.
- El pipeline abre un nuevo trazo de tipo `ERASE`.
- La capa de imagenes y fondo no se toca.
- Exportacion replaya erase como operacion sobre tinta, no como pintura blanca.

## Compatibilidad Con canvas-state.json v2

- T123B no cambia sidecar.
- T124 tampoco deberia cambiarlo todavia.
- Si Fase 3 agrega presion:
  - seguir leyendo puntos antiguos sin `pressure`;
  - default `pressure=1.0`;
  - no borrar `strokes` heredado;
  - conservar fallback PNG antiguo.

## Criterios De Aceptacion

- El lienzo funciona igual con JavaFX fallback.
- La entrada de tinta ya no esta cableada directamente en `TechnicalProblemDialog` mediante handlers de mouse sobre `drawingSurface`.
- `StudyProblemCanvasSurface` no importa `MouseEvent`, `StylusEvent` ni clases de lectureStudio.
- Hay diagnostico accionable de muestras/latencia/distancia.
- La app no falla si el provider nativo no existe o no carga.
- Los problemas viejos siguen abriendo.
- `Guardar`, `Guardar cambios`, `Guardar + exportar PNG` siguen funcionando.
- El borrador no mancha imagenes.
- La exportacion HD sigue usando el modelo vectorial disponible.

## Comandos De Prueba

T123B:

```powershell
mvn -q -DskipTests compile
mvn -q -Dtest=TechnicalProblemT122SourceTest test
mvn -q test
```

T124:

```powershell
mvn -q -DskipTests compile
mvn -q test
```

Si se decide usar profile experimental:

```powershell
mvn -q -Pstylus -DskipTests compile
mvn -q -Pstylus test
```

## Riesgos Maven / Nativos / Windows

- El modulo base del repo `lectureStudio/stylus` declara `module stylus`.
- Hay que confirmar el nombre real del modulo en el jar publicado antes de tocar `module-info.java`.
- `stylus-javafx` existe como modulo/subproyecto, pero la API efectiva a usar debe confirmarse con el jar Maven, no solo con el repositorio.
- `StylusManager` carga una libreria nativa llamada `stylus`; puede fallar si el jar no trae el binario correcto o si Windows/JDK no lo acepta.
- El provider nativo debe estar rodeado de `try/catch(Throwable)` en la frontera de carga, no dentro del render.
- Si JavaFX sigue coalesciendo eventos aunque haya provider nativo, el diagnostico debe mostrarlo antes de invertir mas en render.

## Fuentes Tecnicas Consultadas

- Repositorio `lectureStudio/stylus`: https://github.com/lectureStudio/stylus
- Codigo fuente verificado localmente desde el repo:
  - `org.lecturestudio.stylus.StylusEvent`
  - `org.lecturestudio.stylus.StylusAxesData`
  - `org.lecturestudio.stylus.StylusCursor`
  - `org.lecturestudio.stylus.StylusManager`
  - `org.lecturestudio.stylus.StylusListener`
- Artifacts a verificar en Maven Central para T124:
  - `org.lecturestudio.stylus:stylus-javafx:0.3.0`
  - `org.lecturestudio.stylus:stylus:0.3.0`

## Proximo Prompt Sugerido

```text
PLEASE IMPLEMENT THIS PLAN:
# T123B: Separacion Minima De Entrada De Tinta y Diagnostico

## Summary
- Crear la frontera `InkInputProvider` para desacoplar MouseEvent del lienzo tecnico.
- Mantener comportamiento visual actual, sin integrar todavia lectureStudio/stylus.
- Agregar diagnostico real de muestras, distancia y latencia para comprobar si JavaFX MouseEvent es el cuello de botella.
- No cambiar `.docupodcast.json`, no tocar DMS/UENS, no tocar OCR/PDF/ComfyUI.

## Key Changes
- Crear paquetes `presentation.document.ink.input`, `pipeline`, `diagnostics` y `geometry` si son necesarios.
- Crear `InkInputProvider`, `InkInputListener`, `InkInputSample`, `InkInputCapabilities`.
- Crear `JavaFxMouseInputProvider`.
- Mover los handlers `MouseEvent` de tinta desde `TechnicalProblemDialog.initializeCanvas()` al provider.
- `TechnicalProblemDialog` solo recibe samples semanticamente y los pasa al pipeline/render actual.
- Agregar `InkDiagnostics` con logs opcionales para raw samples/sec, distancias, tiempos y drops.
- Mantener sidecar `canvas-state.json` v2 igual.

## Test Plan
- `mvn -q -DskipTests compile`
- tests focales de `TechnicalProblemDialog` y `StudyProblemCanvasSurface`
- `mvn -q test`
```
