# Tanda 115 - PDF Seleccion, Navegacion y Playbar

## Que se implemento

- La seleccion rectangular PDF calcula coordenadas desde el rectangulo visible real de la pagina renderizada y ya no depende de coordenadas internas fragiles del `ImageView`.
- La captura PDF conserva `sourcePage`, `bbox` y renderiza desde la frontera de aplicacion existente.
- El panel Problema mantiene lenguaje de producto: capturas PDF seleccionadas, no `bbox` ni chunks.
- El indice PDF con origen `Paginas del PDF` evita mostrar cientos de entradas como lista principal y agrega control numerico `Ir a pagina`.
- `Ir a pagina` valida pagina dentro del rango conocido y salta al bloque navegable mas cercano.
- El porcentaje `Documento N%` para PDF sigue calculandose por scroll visual/pagina dominante.
- La pestana Estudio agrega `Desplazar playbar` como configuracion rapida.
- El playbar puede alternarse entre flotante sobre el documento y acoplado al rail izquierdo; tambien se conserva el acople automatico cuando el SideDock derecho esta abierto.

## Que quedo fuera

- Los marcadores persistentes de pagina por proyecto no quedaron implementados en esta tanda.
- No se cambio `.docupodcast.json` para marcadores.
- No se agrego OCR nuevo ni busqueda nueva.

## Decisiones tecnicas

- La conversion de seleccion PDF usa bounds visibles de la pagina y dimensiones reales de la imagen renderizada, para funcionar con zoom, ajuste a ancho y scroll.
- El salto numerico de pagina reutiliza las entradas existentes del outline PDF y selecciona el bloque navegable mas cercano.
- El toggle de playbar se agrego como comando de ribbon, sin introducir un sistema paralelo de controles.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/StudyRibbonT93SourceTest.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test`

## Proxima tarea exacta

T116: implementar marcadores PDF por proyecto: crear marcador desde pagina actual, pedir nombre, persistirlo de forma compatible, listarlo en el panel Indice, saltar a pagina y eliminarlo.
