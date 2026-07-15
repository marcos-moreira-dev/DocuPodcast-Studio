# Tanda 97 - Hotfix Estudio: imagenes, SideDock y lienzo seguro

## Que se implemento

- `SourceVisualBlockView` ahora renderiza imagenes embebidas con un host responsivo transversal. La imagen se ajusta al ancho util del contenedor y conserva proporcion, sin `fitWidth` ni `fitHeight` fijos.
- El comando `PREPARE_TECHNICAL_PROBLEM` desde cinta/menu abre el panel derecho de Problema tecnico y no activa la seleccion de fragmentos.
- `DocuPodcastShellViewModel` agrega `openTechnicalProblemPanel()`, que abre el lector, expande el rail derecho y deja la seleccion controlada por el panel.
- `DocumentTechnicalProblemPanel` elimina previews grandes de texto para el nuevo problema, usa estado compacto, renombra el toggle a `Seleccionar fragmentos para asociar a problema` y apila acciones verticalmente.
- `TechnicalProblemDialog` usa titulo/header `Problema tecnico`, muestra imagenes reales del enunciado si hay crop o `embeddedImageBase64`, y mantiene `Transferir imagen al lienzo`.
- El lienzo del dialogo dejo de ser un `Canvas` monolitico. Ahora usa `TiledCanvasSurface`, con tiles de `Canvas` acotados, crecimiento por filas/columnas y exportacion compuesta a un solo `WritableImage`.
- La exportacion PNG incluye trazos e imagenes transferidas; las imagenes siguen siendo interactivas solo cuando `Interactuar con imagenes` esta activo.

## Que quedo fuera

- OCR y LaTeX editable siguen fuera.
- No se cambia `.docupodcast.json` ni se guardan objetos de imagen editables dentro del JSON.
- No se implementa galeria/detalle de problemas guardados; queda para la tanda siguiente.
- El lienzo tiene limites tecnicos de filas/columnas para evitar volver a saturar JavaFX con texturas enormes.

## Decisiones tecnicas

- La correccion de imagenes se hizo en `SourceVisualBlockView`, no en una vista local, porque el contrato es transversal para visuales fuente.
- El comando de cinta se separo del modo checkbox: abrir panel y seleccionar fragmentos son acciones distintas.
- El crash reportado (`NGCanvas$RenderBuf.validate` con `RTTexture` nulo) se abordo evitando que un unico `Canvas` crezca indefinidamente.
- La compatibilidad se mantiene entregando un unico `WritableImage` al workflow de guardado/exportacion.

## Archivos tocados

- `SourceVisualBlockView`
- `DocumentTechnicalProblemPanel`
- `TechnicalProblemDialog`
- `DocuPodcastShellView` y `DocuPodcastShellViewModel`
- `AppCommandRegistry`
- CSS de `study-problem` y `source-visual`
- Tests fuente de estudio documental
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q '-Dtest=StudyDocumentUxT95SourceTest,StudyDocumentUxT96SourceTest,StudyDocumentUxT97SourceTest,SourceVisualBlockFullscreenSourceTest' test`
- `mvn -q test`

## Proximos pasos

1. Probar manualmente un DOCX con imagen: abrir documento, verificar ajuste transversal de imagen, abrir panel Problema, seleccionar fragmento visual y transferir imagen al lienzo.
2. Tanda 98: vista detalle/galeria de problemas con miniaturas de crops y solucion PNG.
