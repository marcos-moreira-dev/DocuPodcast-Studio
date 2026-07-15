# Tanda 113 - Flujo PDF visual limpio y nitido

## Implementado

- El visor PDF ahora usa zoom visual real: el ancho mostrado de la pagina cambia con el zoom en vez de mantener siempre ajuste a ancho.
- El DPI de render se calcula desde el ancho visible de la pagina con margen de nitidez para evitar que JavaFX estire una imagen raster pequena.
- Se habilito scroll horizontal cuando el zoom visual supera el ancho disponible.
- Se desactivo el subsampling de PDFBox para priorizar fidelidad en paginas escaneadas o con imagenes.
- El render de paginas PDF ahora usa cola limitada, concurrencia maxima de dos trabajos, recorte de cola y revision de render para ignorar resultados obsoletos tras scroll/zoom/documento.
- La seleccion rectangular se captura desde el frame de pagina y convierte coordenadas al `ImageView`, para que el drag no dependa de caer exactamente sobre el nodo imagen.
- En PDF visual, el indicador `Documento N%` ahora sigue la posicion del scroll del visor; en DOCX/Markdown/TXT conserva el calculo por bloque seleccionado.
- El panel Indice deja de mostrar `Buscar con OCR local` como accion primaria y usa `Mejorar busqueda`.
- El fallback sin temario se presenta como `Paginas del PDF`.
- El panel Problema habla de capturas PDF seleccionadas en vez de regiones/bbox/chunks.
- Se corrigieron reglas CSS invalidas `-fx-alignment: stretch` que generaban warnings JavaFX.

## Fuera De Alcance

- Nueva UI de zoom PDF dedicada en la barra inferior.
- OCR masivo automatico de todo el libro.
- Reindexacion semantica completa del temario.
- Cambios de persistencia `.docupodcast.json`.
- Cambios en Domain Model Studio/UENS.

## Decisiones Tecnicas

- Presentation sigue sin importar `PdfBoxRenderEngine`.
- La calidad se mejora renderizando a DPI suficiente para el ancho visible, no aumentando ciegamente el DPI de todas las paginas.
- El OCR queda como ayuda interna de busqueda/deteccion de texto; la UI lo presenta como mejora de busqueda.
- La cola limitada evita que el scroll rapido deje muchos renders antiguos ejecutandose y pintando tarde.
- La seleccion PDF usa handlers en el frame de pagina para conservar el gesto aunque el cursor cambie de nodo durante el arrastre.
- El porcentaje de documento en PDF se calcula por avance visual del `ScrollPane`, porque el lector central ya no depende de chunks visibles.

## Archivos Tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxRenderEngine.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildDocumentOutlineUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/DocumentOutlineOrigin.java`
- `src/main/resources/css/document/pdf-visual-viewer.css`
- `src/main/resources/css/document/study-problem.css`
- `src/main/resources/css/audio-jobs.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualFlowT113SourceTest.java`
- `docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests Ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfVisualFlowT113SourceTest,PdfVisualWorkspaceT102SourceTest,PdfRegionTechnicalProblemT103T104SourceTest,PdfOutlineSearchT111T112SourceTest,PdfBoxRenderEngineTest,CssModularitySourceTest,StatusBarReadingZoomT102SourceTest" test`
- `mvn -q test`

## Proximo Paso

T114: aplicar y cerrar la politica final por modo de proyecto: Estudio documental con PDF potente; Teatro/Video narrativo con PDF como referencia y DOCX/Markdown recomendados para guion editable.
