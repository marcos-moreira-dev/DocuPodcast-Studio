# Tanda 95 - Correccion UX estudio, indice PDF y export preview

## Que se implemento

- Se agrego `DocumentStudySideDock` como carcasa derecha dedicada para estudio documental, siguiendo el patron de `TheatreSideDock`: rail derecho, ancho colapsado/expandido y sincronizacion con `documentRightRailVisibleProperty`.
- `DocumentWorkspaceView` usa el nuevo dock derecho para `DOCUMENT_TECHNICAL_PROBLEM`; el dock izquierdo queda para indice, fragmento y audio/reproduccion.
- El importador PDF conserva `sourcePageCount` en bloques nativos, bbox y fallback visual.
- `BuildDocumentOutlineUseCase` usa `sourcePageCount` para crear fallback de navegacion por todas las paginas PDF conocidas, mapeando paginas sin bloque exacto al bloque util mas cercano.
- `TechnicalProblemDialog` cambio a `SplitPane`, con divisor ajustable entre enunciado y solucion.
- El enunciado del problema se muestra como texto continuo y los crops/imagenes fuente se presentan sin repetir ID/pagina por fragmento.
- Las imagenes fuente pueden transferirse al lienzo. En el lienzo se pueden seleccionar, mover, ampliar, reducir o eliminar cuando `Interactuar con imagenes` esta activo.
- Con `Interactuar con imagenes` apagado, el usuario puede escribir encima de las imagenes sin que capturen el puntero.
- En modo `dibujar`, los scrollbars del lienzo quedan ocultos/no operables; en modo `panear`, se habilitan.
- El lienzo crece automaticamente hacia abajo y hacia la derecha al acercarse a los bordes o al final del scroll.
- La exportacion PNG del problema incluye trazos e imagenes superpuestas en el tamano completo del lienzo.
- `ExportCenterDialog` ahora tiene scroll vertical en la columna derecha y la preview documental elimina el panel interno translucido.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Persistencia JSON de imagenes del lienzo como objetos editables independientes; se fusionan en el PNG de solucion.
- Galeria/detalle visual de problemas guardados.

## Decisiones tecnicas

- `sourcePageCount` se guarda como metadato de bloques PDF para no cambiar `.docupodcast.json`.
- El fallback por paginas PDF ya no se limita a `FLAT_NAVIGATION_LIMIT`; usa el conteo real conocido del PDF.
- Las imagenes del lienzo viven como nodos JavaFX superpuestos sobre el canvas durante la edicion y se combinan al tomar snapshot final.
- Undo/redo sigue aplicando a trazos del canvas; las operaciones de imagen se controlan con seleccion y botones dedicados.
- La preview de exportacion sigue siendo hipotetica: no genera assets ni audio.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildDocumentOutlineUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfDocumentImporter.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java`
- CSS de estudio, side dock y export center.
- Tests de documento, PDF, exportacion y guardarrailes de estudio.
- Documentacion de estudio documental, PDF, indice y export center.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildDocumentOutlineUseCaseTest,StudyDocumentUxT95SourceTest,ExportCenterT95SourceTest,PdfImporterPageCountT95SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 96: vista detalle/galeria de problemas con miniaturas de crops fuente y solucion PNG.
2. Agregar previsualizacion ampliada de solucion guardada sin abrir el editor completo.
3. Evaluar persistencia futura de objetos del lienzo solo si se necesita re-editar imagenes transferidas despues de guardar/cerrar.
