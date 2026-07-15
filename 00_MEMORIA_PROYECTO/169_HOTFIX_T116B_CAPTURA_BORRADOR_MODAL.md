# Hotfix T116B - Captura PDF, borrador real y UX del modal

## Implementado

- `PdfVisualDocumentView` mantiene el overlay de seleccion sobre la pagina PDF y agrega fallback por `frame.sceneToLocal(...)` cuando el overlay no tiene bounds validos.
- La captura PDF ahora puede iniciar, dibujar rectangulo y finalizar desde overlay o desde el frame de pagina, siempre convirtiendo desde la imagen visible a `PdfViewportSelection`.
- `StudyProblemCanvasSurface` agrega `eraseLine(...)`: el borrador usa `AlphaComposite.Clear` sobre la capa de tinta y refresca solo los tiles afectados.
- `TechnicalProblemDialog` deja de usar el color de fondo como borrador. En modo borrador elimina solo tinta; imagenes y fondo permanecen intactos.
- Undo/redo restaura solo la capa de tinta. Ya no repinta fondo ni mezcla imagenes al restaurar snapshots.
- El modal reordena las acciones del enunciado en una barra superior: `Cargar imagen externa...`, `Transferir todas al lienzo` y `Ocultar enunciado`.
- Al ocultar el enunciado, queda visible un boton `Mostrar enunciado` en el lado del lienzo para restaurar el panel.

## Fuera de alcance

- OCR nuevo.
- Cambios en `.docupodcast.json`.
- Persistencia editable de objetos de imagen en JSON.
- Cambios en Domain Model Studio/UENS.

## Decisiones tecnicas

- La ruta principal de seleccion PDF sigue siendo el overlay porque permite capturar sobre la pagina renderizada sin acoplarse a `ImageView`.
- El fallback se calcula desde coordenadas de escena al frame para cubrir layouts donde el cursor aparece, pero el overlay no recibe dimensiones utiles.
- El raster interno de cada tile es la fuente confiable para borrar tinta. Tras aplicar `AlphaComposite.Clear`, se refresca el canvas visible de ese tile desde el raster.
- El borrador no modifica la capa de imagenes ni la capa de fondo; por eso escribir encima de imagenes y borrar despues no debe mancharlas.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/resources/css/document/study-problem.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemHotfixT116BSourceTest.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemHotfixT116BSourceTest,PdfVisualFlowT113SourceTest,PdfRegionTechnicalProblemT103T104SourceTest,StudyDocumentUxT98SourceTest,StudyProblemExportT107SourceTest" test`
- `mvn -q test`

## Proximos pasos

1. Validar manualmente captura PDF con zoom 78%, 100% y superior sobre el libro de matematicas real.
2. Probar borrador sobre imagen transferida: dibujar, borrar, deshacer, rehacer y exportar PNG.
3. Implementar marcadores persistentes de pagina PDF por proyecto: crear, nombrar, ir y eliminar.
