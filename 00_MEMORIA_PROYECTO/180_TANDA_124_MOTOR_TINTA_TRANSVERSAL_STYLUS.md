# Tanda 124 - Motor Transversal De Tinta + Adaptador Stylus

Fecha: 2026-07-01

## Que Se Implemento

- Se creo el paquete puro `application.ink` para modelar tinta reutilizable sin depender del modal de problema tecnico:
  - `InkWorkspaceState`
  - `InkStroke`
  - `InkPoint`
  - `InkPlacedImage`
  - `InkImageCrop`
  - `InkWorkspaceBounds`
  - `InkWorkspaceStateSerializer`
- Se creo la frontera reusable `presentation.ink`:
  - `InkStrokePipeline` para resampling/suavizado compartido.
  - `InkInputProvider`, `InkInputSample`, `InkInputListener`, `InkInputCapabilities` y tipos de cursor.
  - `JavaFxMouseInputProvider` como fallback permanente.
  - `LectureStudioStylusInputProvider` aislado detras de reflexion y fallback, sin dependencia dura.
  - `InkInputProviderFactory` para seleccionar proveedor nativo opcional o JavaFX.
- `TechnicalProblemDialog` queda como primer consumidor del motor transversal:
  - importa la nueva frontera `presentation.ink.input`.
  - serializa sidecar con `InkWorkspaceStateSerializer`.
  - usa `InkStrokePipeline` para resampling compartido.
- `StudyProblemCanvasSurface` expone conversiones hacia/desde `application.ink` para conservar trazos editables.
- Se agrego `TheatreFrameSketchInkHook` como punto de integracion futuro para bocetos teatrales sin implementar UI de teatro.
- Se exportaron los nuevos paquetes en `module-info.java`.

## Que Quedo Fuera

- No se agrego dependencia dura a `lectureStudio/stylus`.
- No se activo presion real ni cursor borrador nativo; eso queda para T125 con pruebas fisicas de tableta.
- No se implemento UI de bocetos teatrales.
- No se cambio `.docupodcast.json`.
- No se implemento OCR, cambios PDF ni cambios de Domain Model Studio/UENS.

## Decisiones Tecnicas

- La tinta queda separada en tres niveles:
  - `application.ink`: modelo puro y sidecar.
  - `presentation.ink`: input, pipeline y frontera JavaFX reusable.
  - consumidor actual: problema tecnico, que solo orquesta UI y persistencia del caso de uso.
- El sidecar generico queda en version 3 y mantiene compatibilidad con sidecars previos y problemas antiguos que solo tienen PNG.
- El proveedor stylus se detecta por reflexion para evitar que una libreria nativa falle al arrancar la aplicacion.
- Teatro no depende de estudio documental; ambos podran compartir `InkWorkspaceState` y el futuro renderer transversal.

## Archivos/Sistemas Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/*`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/*`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/*`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/module-info.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/ink/InkWorkspaceStateSerializerTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT122SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT124InkArchitectureSourceTest.java`
- `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests Ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=InkWorkspaceStateSerializerTest,TechnicalProblemT123BInkInputProviderSourceTest,TechnicalProblemT124InkArchitectureSourceTest" test`
- `mvn -q "-Dtest=TechnicalProblemT122SourceTest,InkWorkspaceStateSerializerTest,TechnicalProblemT124InkArchitectureSourceTest" test`
- `mvn -q test`

## Proximos Pasos

1. T125: probar tableta real y afinar latencia, presion, cursor borrador y tolerancia de muestras con el nuevo `InkInputProvider`.
2. Decidir si `lectureStudio/stylus` entra como dependencia real, perfil opcional o runtime externo despues de validar su empaquetado nativo.
3. Extraer gradualmente render/export/seleccion desde `StudyProblemCanvasSurface` hacia componentes reutilizables de `presentation.ink`.
4. Preparar una tanda futura de teatro para que `TheatreFrameSketch` guarde bocetos manuales con `InkWorkspaceState`.
