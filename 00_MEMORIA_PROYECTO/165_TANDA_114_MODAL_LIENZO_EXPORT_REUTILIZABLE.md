# Tanda 114 - Modal, Lienzo y Exportacion Fiel

## Que se implemento

- El modal `Problema tecnico` reemplaza el selector numerico de grosor por un slider con vista previa circular del trazo.
- El campo superior queda tratado como `Titulo del problema`.
- El titulo ahora se muestra dentro del lienzo como banda superior mientras el usuario dibuja, para evitar escribir encima.
- La exportacion PNG quema ese titulo dentro del lienzo y compone fondo opaco, tinta e imagenes transferidas.
- La exportacion excluye aura/manijas de seleccion y evita agregar zonas negras por transparencias.
- Al reabrir un problema antiguo con `STUDY_SOLUTION_IMAGE`, la imagen compuesta se carga como capa base para seguir dibujando encima.
- La pantalla completa conserva la restauracion del titulo, split, notas y controles del modal.
- Se corrigio el CSS invalido de alineacion que generaba warnings de JavaFX.

## Que quedo fuera

- Los problemas nuevos todavia guardan las imagenes transferidas dentro del PNG compuesto final. No se agrego persistencia JSON de objetos editables independientes para mover/redimensionar imagenes despues de cerrar y reabrir.
- No se cambio el esquema `.docupodcast.json`.
- No se agrego OCR ni cambios en lectura PDF.

## Decisiones tecnicas

- El titulo se quema dentro del mismo bitmap exportado, no como alto adicional externo, para que el lienzo visto y el PNG resultante coincidan.
- La solucion antigua se recupera como base dibujable. Es compatible y evita ocultar la solucion compuesta aunque falten metadatos editables.
- El grosor usa `Slider` transversal con preview visual en vez de `Spinner`.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/StudioFormControls.java`
- `src/main/resources/css/document/study-problem.css`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test`

## Proximos pasos

1. Persistir metadatos editables de imagenes transferidas si se decide cambiar el contrato de proyecto.
2. Mantener la solucion PNG compuesta como fallback obligatorio para problemas antiguos o incompletos.
3. Validar manualmente exportacion con imagenes, trazos finos y titulo largo.
