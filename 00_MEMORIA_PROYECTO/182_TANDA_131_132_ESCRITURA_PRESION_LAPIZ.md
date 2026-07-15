# Tanda 131-132 - Escritura Siempre Funcional Y Presion De Lapiz

## Que Se Implemento

- El modo `Dibujar` ahora prioriza la capa de tinta por encima de imagenes y crops colocados.
- `Interactuar con imagenes` queda como edicion explicita: en modo dibujo normal la capa de imagenes no roba eventos de escritura.
- Al cambiar entre lienzo/texto o panear/dibujar se recalcula el modo de interaccion de imagenes para evitar bloqueos de tinta.
- El motor `InkRealtimeStrokeEngine` conserva la presion de cada punto y ya no parte un trazo por cambios de grosor derivados de presion.
- El sidecar de tinta conserva `pressure` por punto, con compatibilidad hacia estados previos que no tenian ese campo.
- El pincel unico usa la formula acordada de presion solo cuando el proveedor activo declara entrada nativa real; con JavaFX mouse se usa `pressure=1.0` y grosor constante.

## Decisiones Tecnicas

- La app no debe mentir sobre capacidades: `Entrada: JavaFX mouse` significa sin presion real y sin borrador nativo.
- La presion no cambia `.docupodcast.json`; vive en el sidecar editable del lienzo.
- La prioridad de escritura se resuelve en la capa de eventos, no eliminando soporte de seleccion/redimension de imagenes.
- Si la tableta sigue reportando `JavaFX mouse`, el siguiente trabajo debe ser validar Windows Pointer/Wintab real antes de seguir ajustando suavizado.

## Archivos Tocadas

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/InkPoint.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT131T132InkPrioritySourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurfaceT132PressureStateTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngineT132PressureSourceTest.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`
- `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`

## Tests Ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemT131T132InkPrioritySourceTest,InkRealtimeStrokeEngineT132PressureSourceTest,StudyProblemCanvasSurfaceT132PressureStateTest" test`

## Quedo Fuera

- Validacion fisica de Windows Pointer con la tableta real del usuario.
- Implementacion funcional de Wintab si Windows Pointer no entrega paquetes nativos.
- Cambios en OCR, PDF, DMS/UENS o esquema principal del proyecto.

## Proximos Pasos

1. Validar con tableta real si la UI cambia de `Entrada: JavaFX mouse` a `Entrada: Windows Pointer`.
2. Si sigue en JavaFX mouse, implementar Wintab real como fallback nativo.
3. Medir puntos por segundo, latencia input-preview y respuesta de botones durante 10 segundos de escritura rapida.
