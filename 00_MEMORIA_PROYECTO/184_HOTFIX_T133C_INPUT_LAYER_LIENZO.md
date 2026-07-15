# Hotfix T133C - Input layer del lienzo tecnico

## Que se implemento

- Se conecto `InkInputProvider` directamente a `drawingSurface.inkInputLayer()`, que es la capa superior real de captura del lienzo.
- `inkInputLayer` ahora usa `setPickOnBounds(inkActive)` mientras el modo dibujo o seleccion de region esta activo, para que el area blanca y las imagenes no dejen al provider sin eventos.
- Se mantuvo el orden native-first del input: `WindowsPointerInkInputProvider`, `WintabInkInputProvider`, `LectureStudioStylusInputProvider` y recien al final `JavaFxMouseInputProvider`.
- Se actualizaron los tests fuente que protegian el acoplamiento viejo a `drawingSurface`.

## Que quedo fuera

- No se cambio la persistencia `.docupodcast.json`.
- No se implemento OCR ni cambios PDF.
- No se toco DMS/UENS.
- No se reescribio el backend nativo de presion; esta memoria solo corrige que la escritura vuelva a entrar por la capa correcta.

## Decision tecnica

El provider debe engancharse a la capa de input superior, no al `StudyProblemCanvasSurface` completo. Asi las manijas de imagen pueden seguir recibiendo eventos especificos, pero el area del lienzo conserva una superficie transparente que captura escritura cuando el modo activo es dibujar.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT123BInkInputProviderSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT131T132InkPrioritySourceTest.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemT123BInkInputProviderSourceTest,TechnicalProblemT131T132InkPrioritySourceTest,StudyProblemCanvasSurfaceT133InkHitAreaSourceTest,TechnicalProblemT129T130NativeInkSourceTest" test`
- `mvn -q test`

## Proximos pasos

1. Validar manualmente en la ventana `Problema tecnico` que se pueda dibujar sobre blanco y sobre imagen seleccionada.
2. Si `Entrada: JavaFX mouse` sigue apareciendo con tableta, diagnosticar por que `WindowsPointerInkInputProvider` o `WintabInkInputProvider` no estan recibiendo paquetes reales.
3. Seguir con la tanda de presion real del lapiz, pero sin volver a bloquear el fallback de escritura basica.
