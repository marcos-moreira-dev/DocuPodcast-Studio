# DocumentSource unificado y PDF en estudio documental

## Decision vigente

DocuPodcast Studio abre documentos fuente en modo solo lectura para convertirlos en material de estudio. El frente puede ofrecer:

- Word/DOCX;
- PDF con texto nativo extraible u OCR local cuando no haya texto suficiente;
- PDF escaneado o visual en modo limitado;
- Markdown/MD;
- TXT.

Si contiene formulas, tablas o layout matematico dificil, la primera estrategia de producto sigue siendo conservar evidencia visual/crops del PDF antes de intentar interpretar texto. OCR local existe como capa interna de deteccion de texto, no como reconstruccion de LaTeX.

Desde Tanda 99 existe un motor visual embebido basado en Apache PDFBox. Ese motor renderiza paginas completas del PDF como imagen fiel de pagina y queda disponible para cualquier tipo de proyecto. Todavia no reemplaza la interfaz del workspace Documento; prepara el contrato para hacerlo solo cuando la fuente sea PDF.

Desde Tanda 100 se declara una politica transversal por modo de proyecto:

- `DOCUMENTARY_STUDIO`: PDF visual de primera clase. El camino objetivo es pagina renderizada, seleccion rectangular de regiones, crops fieles y capa textual nativa/OCR gradual para lectura, busqueda y resaltado.
- `THEATRE_PRODUCTION`: PDF permitido como referencia visual de solo lectura. Para guion teatral editable se recomienda DOCX o Markdown.
- `NARRATIVE_VIDEO`: PDF permitido como referencia visual de solo lectura. Para guion narrativo editable se recomienda DOCX o Markdown.

La app no edita ni convierte internamente el PDF en guion. Si el usuario necesita modificar el contenido, debe editar o convertir el archivo fuera de DocuPodcast Studio y volver a abrir/refrescar la fuente.

## Regla PDF vigente

Cuando el usuario selecciona un PDF, el sistema intenta extraer texto nativo.

- Si el PDF contiene texto nativo suficiente, se importa como documento narrable de solo lectura con metadatos de pagina, `sourcePageCount`, `extractionMode`, `confidence`, disponibilidad de render visual y, cuando Poppler lo permite, `bbox`, `bboxUnits`, `pageWidth` y `pageHeight`.
- Si el PDF es escaneado, imagen o no contiene texto extraible suficiente, la app intenta OCR local por pagina con Tesseract para crear bloques narrables con `bbox`, pagina, confianza y origen `OCR_LOCAL`.
- Si OCR local falla o no esta disponible, la app abre un documento visual limitado con un bloque visual por pagina y `sourcePageCount` confiable desde el motor embebido cuando PDFBox puede inspeccionarlo. En el fallback visual no se modifica el PDF fuente. Las notas, problemas, crops y soluciones viven en el proyecto.

## Contrato tecnico

El flujo usa `PdfDocumentImporter`.

- Usa `PdfBoxRenderEngine` para inspeccionar paginas y renderizar visualmente PDFs sin depender de herramientas externas.
- La API transversal vive en `application.document`: `PdfRenderEngine`, `PdfDocumentInfo`, requests/resultados de render, errores controlados y `BuildPdfVisualDocumentUseCase`.
- `PdfBoxRenderEngine` vive en `infrastructure.document`, usa Apache PDFBox 3.0.7 y renderiza con fondo opaco para evitar transparencias negras.
- `jbig2-imageio` queda disponible para PDFs escaneados con imagenes JBIG2.
- `ProjectPdfSourcePolicy` centraliza la decision de uso PDF por modo de proyecto.
- Intenta `pdftotext -bbox-layout` mediante el runner comun de procesos cuando esta disponible.
- Si `-bbox-layout` falla o no produce texto suficiente, cae a `pdftotext -layout`.
- Mantiene fallback Java nativo para texto simple.
- Materializa bloques PDF con `sourcePage`, `sourcePageCount`, `visualRenderAvailable`, `visualRenderEngine`, `bbox`, `bboxUnits`, `pageWidth`, `pageHeight`, `extractionMode` y `confidence`.
- Desde Tanda 93, los bloques `bbox-layout` se ordenan por coordenadas y se unen cuando Poppler entrega fragmentos cercanos de la misma columna.
- Los problemas tecnicos generan crops fuente con PDFBox por defecto desde `sourcePage` + `bbox`, guardados como assets `STUDY_SOURCE_CROP`; `pdftoppm` queda como fallback si el render embebido falla.
- El indice documental robusto usa esos metadatos sin cambiar el proyecto: intenta bookmarks PDF no cifrados, luego paginas `Contents/Table of Contents`, luego secciones inferidas, luego anclas por pagina PDF y finalmente navegacion plana.
- Desde Tanda 95, las anclas por pagina usan `sourcePageCount` para listar todas las paginas conocidas del PDF aunque Poppler solo haya extraido bloques navegables en algunas paginas; si una pagina no tiene bloque exacto, salta al bloque util mas cercano.
- Para PDFs sin texto suficiente emite `pdf-ocr-applied` cuando OCR crea bloques narrables, o `pdf-ocr-unavailable` + `pdf-visual-only` cuando debe conservar fallback visual.

## Seleccion visual por regiones

Tanda 100 agrega el contrato puro para seleccionar regiones sobre una pagina PDF renderizada:

- `PdfViewportSelection` representa el rectangulo dibujado por el usuario sobre la pagina renderizada en pixeles de viewport.
- `PdfPageRegion` convierte ese rectangulo a puntos PDF (`sourcePage`, `bbox`).
- `CapturePdfVisualRegionUseCase` usa `PdfRenderEngine.renderCrop(...)` para materializar un PNG de esa region.
- `PdfRegionCaptureResult` devuelve pagina, `bbox`, DPI, tamano final y ruta PNG.

Desde Tanda 103, el visor PDF visual central permite activar seleccion rectangular desde el SideDock de Problema. El usuario arrastra sobre la pagina renderizada, la UI convierte las coordenadas visibles a `PdfViewportSelection` y el workspace materializa un PNG temporal por application boundary. La presentacion puede usar estos contratos, pero sigue sin depender de `PdfBoxRenderEngine`.

Desde Tanda 113, la UI nombra este flujo como capturas del PDF: el usuario selecciona capturas visuales para asociarlas a un problema tecnico, mientras `bbox`, OCR y motor de render quedan como detalles internos.

Desde Tanda 115B, la seleccion visual usa un overlay propio sobre la imagen de pagina renderizada. El rectangulo visible se calcula contra el area real de la imagen mostrada, no contra el scroll ni contra coordenadas internas fragiles del `ImageView`.

Desde Hotfix T116B, el visor mantiene ese overlay como ruta principal y agrega fallback por hit-test del frame de pagina. Esto cubre casos donde el cursor de seleccion aparece, pero el `Pane` del overlay tiene bounds inconsistentes por zoom/layout; el drag se convierte igualmente desde la imagen visible a `PdfViewportSelection`.

Desde Tanda 104, las capturas PDF acumuladas crean problemas tecnicos sin checkboxes. Cada captura se registra como asset `STUDY_SOURCE_CROP`, y cada fuente usa `StudySourceReference.visualRegion(...)` con `sourcePage`, `bbox`, `sourceCropAssetId` y `selectedText` vacio o best-effort. Para DOCX/Markdown/TXT sigue existiendo seleccion por bloques.

Desde Tanda 119, estudio documental puede crear una fuente PDF desde una carpeta de imagenes. La operacion requiere proyecto guardado, ordena imagenes compatibles por nombre natural, genera un PDF dentro de `source/generated-pdf-from-images` con una imagen por pagina usando PDFBox y abre ese PDF como fuente documental actual. No convierte imagenes en texto, no ejecuta OCR y no cambia el contrato `.docupodcast.json`.

## Capa textual futura

Se separan dos capas:

- capa visual: pagina PDF renderizada fielmente por PDFBox;
- capa textual: palabras/lineas con coordenadas, confianza y origen.

Tanda 100 deja contratos de preparacion para `PdfTextLayer`, `PdfTextLine`, `PdfTextToken` y `PdfTextLayerOrigin`. Primero se debe usar texto nativo cuando exista (`bbox-layout`); OCR local queda como backend opcional posterior. Las formulas/LaTeX no se reconstruyen: si la capa textual falla, la evidencia confiable es la imagen renderizada o el crop.

Desde Tanda 108, `BuildPdfNativeTextLayerUseCase` construye la primera capa textual nativa desde los metadatos del documento importado. Cada bloque con `sourcePage`, `bbox`, `bboxUnits=pdf-points`, `pageWidth` y `pageHeight` se convierte en una linea `NATIVE_BBOX`; los tokens son aproximados hasta que exista bbox por palabra. Las paginas conocidas sin bbox nativo quedan como `UNAVAILABLE` con warning.

Desde Tanda 109, `BuildPdfVisualReadingProjectionUseCase` une el visor PDF visual con la capa textual nativa. Durante reproduccion, el workspace puede saltar a la pagina y marcar el bloque/linea activo sobre el `ImageView` renderizado sin que presentation importe PDFBox.

Desde Tanda 110, `BuildPdfOcrTextLayerUseCase` expone OCR local por pagina. La implementacion v1 `TesseractPdfOcrEngine` renderiza la pagina con PDFBox, ejecuta Tesseract CLI por `DefaultExternalProcessRunner`, parsea TSV con `PdfOcrTsvParser` y devuelve `PdfTextLayerOrigin.OCR_LOCAL` con palabras, lineas, bbox en puntos PDF y confianza. La importacion PDF lo usa automaticamente cuando no hay texto nativo suficiente; la busqueda OCR explicita sigue existiendo para completar paginas no disponibles con cache.

Desde Tanda 112, `SearchPdfTextUseCase` busca sobre la capa textual resuelta. La busqueda normal usa texto nativo disponible y evita trabajo pesado; la accion explicita de OCR local puede completar paginas `UNAVAILABLE` usando cache por proyecto/PDF/pagina. Cada resultado conserva pagina, texto de contexto, origen `NATIVE_BBOX` u `OCR_LOCAL` y un `PdfVisualTextHighlight` para saltar/resaltar la region sobre el visor PDF.

Desde Tanda 113, el visor PDF se ajusta como superficie de lectura visual y no como prueba tecnica: el zoom cambia el ancho visible de la pagina, el DPI se calcula desde el ancho real mostrado con margen de nitidez, el render se limita a una cola pequena de paginas visibles/cercanas y la accion visible de busqueda OCR se presenta como mejora de busqueda, no como concepto tecnico. El indicador `Documento N%` en PDF sigue la posicion del scroll visual, no el bloque de texto seleccionado.

## Temario PDF

Desde Tanda 111, `BuildPdfEnhancedOutlineUseCase` refuerza el indice PDF cuando el indice base es debil. La prioridad vigente es:

- bookmarks PDF mapeables a pagina;
- paginas `Contents` / `Table of Contents`;
- capitulos y secciones numeradas confiables;
- capa textual nativa/OCR local en paginas iniciales cuando el indice escaneado o pobre necesita ayuda;
- fallback por paginas renderizadas.

No se debe aceptar como temario principal una lista pobre de secciones bibliograficas, referencias o pocas entradas concentradas al final del libro. Si el temario enriquecido detecta una tabla de contenidos con capa OCR, las lineas OCR sinteticas se usan solo para inferir la estructura; las anclas finales siguen apuntando a bloques reales o paginas existentes del documento.

`SourceDocumentRequirementException` sigue existiendo para otros requisitos de fuentes, pero el PDF escaneado ya no se rechaza automaticamente por falta de OCR.

## Fuera de alcance

- reconstruccion LaTeX completa;
- lectura semantica perfecta de layout, tablas, formulas o temarios PDF;
- edicion o sobrescritura del PDF fuente;
- solicitar contrasenas de PDF desde UI.

## Relacion con refrescar contenido

El mismo contrato aplica al refresco: si el usuario modifica o reemplaza el PDF externo, `Refrescar contenido` debe volver a verificar texto nativo. Si ya no hay texto suficiente, puede caer a modo visual limitado sin destruir las capas de estudio existentes.
