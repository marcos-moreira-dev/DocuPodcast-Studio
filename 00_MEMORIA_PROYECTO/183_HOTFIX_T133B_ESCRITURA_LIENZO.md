# Hotfix T133B - Escritura Del Lienzo Recuperada

## Implementado
- Se agrego un area transparente real dentro de `StudyProblemCanvasSurface.inkInputLayer`.
- La entrada de tinta ya no depende solo de un `Pane` vacio con `pickOnBounds`, que en el layout actual podia no recibir eventos de mouse/tableta.
- El area de captura se redimensiona junto con el lienzo logico y queda al fondo de la capa de tinta para no tapar manijas de imagen.
- Se actualizo el test de prioridad de tinta para la semantica actual: la capa de imagen solo captura eventos durante recorte; en modo dibujo la tinta mantiene prioridad.

## Fuera de alcance
- Presion nativa real por Windows Pointer/Wintab.
- Cambios nuevos al esquema `.docupodcast.json`.
- OCR, PDF o DMS/UENS.

## Archivos tocados
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT131T132InkPrioritySourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurfaceT133InkHitAreaSourceTest.java`

## Tests
- Ejecutados:
  - `mvn -q -DskipTests compile`
  - `mvn -q "-Dtest=TechnicalProblemT131T132InkPrioritySourceTest,StudyProblemCanvasSurfaceT133InkHitAreaSourceTest" test`
  - `mvn -q test`
- Resultado: verde. La suite completa solo emitio advertencias de PDFBox sobre fixtures con offsets de stream no exactos.

## Proximo paso
- Validar en la app que en modo `Lienzo/texto: lienzo` + `Panear/dibujar: dibujar` se pueda escribir encima de area blanca e imagenes.
- Si la app escribe con JavaFX mouse pero sigue sin presion, retomar T129/T130 para Windows Pointer/Wintab real.
