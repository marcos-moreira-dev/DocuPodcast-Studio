# Memoria 178 - Plan T123B/T124 Stylus y Motor de Tinta

## Que se hizo

- Se inspecciono el flujo actual de tinta del modal de problema tecnico.
- Se verifico que `TechnicalProblemDialog` aun conecta `MouseEvent` directamente sobre `drawingSurface`.
- Se verifico que `StudyProblemCanvasSurface` ya tiene superficie por tiles, live layer, `InkStrokeState`, `InkPointState`, exportacion por capas y restauracion de sidecar.
- Se reviso la libreria `lectureStudio/stylus` desde su repositorio publico.
- Se dejo un plan tecnico rector en `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`.

## Diagnostico corto

- El problema de escritura lenta no debe resolverse integrando una libreria nativa directamente en el lienzo.
- Primero se debe separar entrada, pipeline, render y persistencia.
- JavaFX `MouseEvent` debe quedar como fallback estable.
- `lectureStudio/stylus` es candidata para T124, pero solo como proveedor experimental detras de adapter + factory + fallback.

## Decisiones tecnicas

- T123B sera desacoplamiento minimo y diagnostico.
- T124 sera integracion experimental de `lectureStudio/stylus`, si Maven/JNI lo permiten.
- No se cambia `.docupodcast.json`.
- No se toca Domain Model Studio/UENS.
- No se rompe `canvas-state.json` v2 ni problemas antiguos.

## Archivos tocados

- `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`
- `00_MEMORIA_PROYECTO/178_TANDA_123B_124_PLAN_STYLUS_TINTA.md`

## Tests ejecutados

- No se ejecutaron tests porque esta tanda fue solo de planificacion y documentacion.

## Proxima tarea exacta

Implementar T123B:

1. Crear `InkInputProvider`, `InkInputListener`, `InkInputSample` e `InkInputCapabilities`.
2. Crear `JavaFxMouseInputProvider`.
3. Mover los handlers `MouseEvent` de tinta fuera de `TechnicalProblemDialog`.
4. Agregar diagnostico real de samples/latencia/distancia.
5. Ejecutar `mvn -q -DskipTests compile` y `mvn -q test`.
