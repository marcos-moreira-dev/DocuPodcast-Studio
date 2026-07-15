# Tanda 98 - Hotfix capas de lienzo y exportacion segura

## Que se implemento
- `TechnicalProblemDialog` separa el lienzo en tres capas: fondo, imagenes transferidas y tinta.
- La tinta queda por encima de las imagenes transferidas, de modo que con `Interactuar con imagenes` apagado el usuario puede escribir sobre la imagen.
- Las imagenes seleccionadas ahora muestran manijas diagonales de redimensionamiento. Al arrastrarlas se conserva la proporcion.
- El crecimiento del lienzo se mantiene por tiles, pero se redujo el limite de filas/columnas para evitar que JavaFX intente crear texturas enormes.
- El historial de undo del lienzo se redujo a 8 snapshots para no acumular imagenes raster gigantes durante ejercicios largos.
- La exportacion PNG compone fondo opaco, imagenes y tinta; recorta al area util con margen y usa escala hasta 4x si entra en el limite seguro.
- Se evita exportar todo el lienzo vacio, corrigiendo el PNG enorme con zonas negras.

## Que quedo fuera
- No se implemento una nueva heuristica profunda de temario/indice PDF.
- No se agrego persistencia editable de objetos de imagen dentro del `.docupodcast.json`.
- No se implemento OCR ni reconstruccion LaTeX.
- No se agrego presion de tableta digitalizadora.

## Decisiones tecnicas
- La capa de tinta (`strokeLayer`) es mouse-transparent para permitir que las imagenes sigan siendo seleccionables cuando el modo de interaccion esta activo.
- La exportacion aplica el orden visual esperado: fondo -> imagenes -> tinta.
- La escala 4x es oportunista: si el PNG excede `MAX_EXPORT_PIXELS`, baja la escala antes de comprometer estabilidad.
- Las manijas de resize usan escapes Unicode en Java para mantener el archivo fuente estable y mostrar flechas diagonales en UI.

## Archivos tocados
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/resources/css/document/study-problem.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyDocumentUxT98SourceTest.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados
- `mvn -q -DskipTests compile`

## Proximos pasos exactos
1. Ejecutar `mvn -q test` y corregir cualquier regresion.
2. Probar manualmente: transferir imagen al lienzo, escribir encima con `Interactuar con imagenes` apagado, seleccionar imagen, usar manijas diagonales y exportar PNG.
3. Siguiente tanda prioritaria: indice/temario PDF robusto real, con deteccion de Contents, bookmarks, capitulos/secciones y descarte de ruido de bibliografia.
