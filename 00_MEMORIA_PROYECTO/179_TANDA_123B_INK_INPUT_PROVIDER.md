# Memoria 179 - Tanda 123B InkInputProvider

## Que se implemento

- Se creo una frontera de entrada para tinta en `presentation.document.ink.input`.
- Se agregaron contratos puros de UI:
  - `InkInputProvider`
  - `InkInputListener`
  - `InkInputSample`
  - `InkInputCapabilities`
  - `InkInputCursor`
  - `InkInputProviderFactory`
- Se agrego `JavaFxMouseInputProvider` como fallback permanente para mouse/tableta que trabaja como mouse.
- `TechnicalProblemDialog` dejo de registrar handlers directos de tinta sobre `drawingSurface` y ahora consume muestras normalizadas desde `InkInputProvider`.
- La muestra de tinta ya contempla presion y cursor borrador, aunque el fallback JavaFX usa `pressure = 1.0`.
- Se detiene el proveedor y el `AnimationTimer` al cerrar el dialogo para evitar listeners vivos despues del modal.

## Que quedo fuera

- No se integro `lectureStudio/stylus`.
- No se agregaron librerias nativas ni cambios de empaquetado.
- No se cambio `.docupodcast.json`.
- No se cambio `canvas-state.json` v2.
- No se modifico Domain Model Studio/UENS.

## Decisiones tecnicas

- El fallback JavaFX sigue siendo obligatorio y permanente.
- Un proveedor nativo posterior debe vivir detras de `InkInputProviderFactory`, no dentro de `TechnicalProblemDialog` ni `StudyProblemCanvasSurface`.
- La presion queda normalizada antes de afectar grosor; si una fuente no reporta presion valida, se usa `1.0`.
- El borrador de stylus queda modelado con `InkInputSample.requestsEraser()`, pero por ahora se combina con el toggle visual `Borrador`.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputCapabilities.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputCursor.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputListener.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputProviderFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputSample.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/JavaFxMouseInputProvider.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT123BInkInputProviderSourceTest.java`
- `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q -Dtest=TechnicalProblemT123BInkInputProviderSourceTest test`
- `mvn -q test`

## Proxima tarea exacta

T124: implementar `LectureStudioStylusInputProvider` o adapter equivalente para Windows Ink/stylus nativo, con carga segura, diagnostico de capacidades, fallback automatico a `JavaFxMouseInputProvider` y pruebas de que la app no falla si la libreria nativa no esta disponible.
