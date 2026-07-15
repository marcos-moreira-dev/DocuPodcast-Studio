# Tanda 127-128 - Stylus real, tinta fluida y auditoria del modal tecnico

## Que se implemento
- El proveedor `LectureStudioStylusInputProvider` ya no declara capacidades nativas cuando en realidad delega a JavaFX mouse.
- `InkInputCapabilities.lectureStudioFallback(...)` ahora reporta `JavaFX mouse`, sin presion, tilt, borrador ni provider nativo.
- El modal de problema tecnico muestra un diagnostico discreto: `Entrada: Stylus nativo` o `Entrada: JavaFX mouse`, con tooltip de capacidades reales.
- `JavaFxMouseInputProvider` usa filtros de eventos y agrega soporte basico de eventos touch como fallback.
- `InkRealtimeStrokeEngine` aumento el presupuesto de drenado por frame, limita puntos por frame y agrega flags apagados para diagnostico de input/render.
- La limpieza de la capa live del trazo ahora toca solo tiles sucios, no todos los tiles del lienzo.

## Que quedo fuera
- No se agrego una dependencia real de LectureStudio ni un puente nativo Windows/JNA.
- No se implemento OCR, cambios de PDF, ni UI de bocetos teatrales.
- No se modifico `.docupodcast.json`.
- No se toco Domain Model Studio/UENS.

## Decision tecnica
- La app debe decir la verdad: detectar clases de una libreria no equivale a capturar eventos nativos de tableta.
- Hasta tener un API nativo concreto, el modo operativo queda como JavaFX mouse/touch fallback.
- El camino caliente de tinta debe seguir moviendose hacia: encolar puntos rapido, drenar por `AnimationTimer`, evitar sidecar/export/snapshot/bounds globales durante drag.

## Archivos tocados
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputCapabilities.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/LectureStudioStylusInputProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/JavaFxMouseInputProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/resources/css/document/study-problem.css`
- tests fuente de tinta/input/arquitectura.

## Tests ejecutados
- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=InkRealtimeStrokeEngineSourceTest,TechnicalProblemT124InkArchitectureSourceTest,TechnicalProblemT123BInkInputProviderSourceTest,TechnicalProblemT122SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos
1. Medir en runtime con `INK_INPUT_DIAGNOSTICS` y `INK_RENDER_DIAGNOSTICS` activables por sistema, usando tableta real.
2. Investigar una API concreta para stylus nativo: LectureStudio si se empaqueta con eventos utiles, o Windows Pointer/JNA aislado.
3. Si JavaFX sigue coalesciendo demasiado, implementar provider nativo real detras de `InkInputProvider` sin tocar el modal.
4. Continuar extrayendo logica pesada de `TechnicalProblemDialog` hacia componentes `presentation.ink`.
