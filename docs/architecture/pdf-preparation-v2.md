# Arquitectura PDF V2

## Estado final

PDF V2 es la única arquitectura documental admitida para PDF. Su fuente de
verdad es `document/manifest.json` junto con un JSON UTF-8 por página. No hay
lector dual, migración del prototipo, `DocumentBlock` PDF ni adaptador
PDF→`ReadableDocument`. Los proyectos PDF preliminares deben reimportarse.

`ReadableDocument` queda reservado para DOCX, TXT y Markdown.

## Fuente de proyecto

`ProjectDocumentSource` es una unión sellada:

- `BlockDocumentSource(ReadableDocument)` para documentos por bloques;
- `PreparedPdfSource(PreparedPdfWorkspaceRef)` para PDF.

`PreparedPdfWorkspaceRef` conserva raíz del workspace, PDF fuente y SHA-256.
El manifest se consulta siempre mediante `PreparedPdfDocumentRepository`, por
lo que la sesión no puede retener un snapshot obsoleto.

Un PDF importado antes del primer guardado vive en un staging V2 administrado.
El primer guardado lo promueve de forma atómica; cancelar o cerrar elimina
exclusivamente ese staging. Guardar como copia fuente, manifest y páginas sin
crear `document/document.json`.

## Persistencia canónica

```text
source/<archivo>.pdf              original intacto y de solo lectura
document/manifest.json            identidad, firma y análisis transversal
document/pages/page-000001.json   preparación canónica de una página
document/pages/page-000042.json
```

- La ausencia del JSON de una página significa `UNPREPARED`.
- `QUEUED` y `PROCESSING` existen únicamente en memoria.
- Cada región conserva texto, geometría, columna, orden, tipo, narratabilidad,
  evidencias, revisión y overrides independientes.
- La decisión efectiva es `override manual → decisión automática`.
- Ninguna heurística elimina una región. `NON_NARRATABLE` y `UNCERTAIN` son
  visibles y buscables; solo `NARRATABLE` llega al TTS.
- Un fallo de reprocesamiento conserva la última página válida.
- El índice de regiones y headings vive únicamente en memoria y se invalida
  al publicar una página.

La publicación escribe un temporal en el mismo directorio, fuerza el canal,
relee y valida el JSON y finalmente usa movimiento atómico, con reemplazo
controlado cuando el sistema de archivos no soporta `ATOMIC_MOVE`.

## Preparación y concurrencia

`PreparePdfPageUseCase` prepara exactamente una página y tiene puntos seguros
de cancelación antes de extraer, clasificar y publicar. Solo
`PdfPagePreparationScheduler` puede invocarlo.

El scheduler mantiene:

- un trabajador y una publicación simultánea;
- clave `(sourceSha256, pageNumber)`;
- prioridades `URGENT`, `HIGH`, `NORMAL`, `BACKGROUND`;
- FIFO, deduplicación y promoción de tareas;
- resultado compartido para solicitudes equivalentes;
- pausa entre páginas, cancelación pendiente y cancelación cooperativa activa.

`PdfProjectSessionToken` impide callbacks cruzados. El adaptador del viewport
usa debounce de 250 ms y solicita la visible, dos anteriores y cinco
siguientes. El clic manual es siempre `URGENT`.

Toda operación multipágina declara un `PdfPreparationScope`: página, sección,
resto de sección, rango o documento completo. Una búsqueda ordinaria consulta
solo lo preparado y nunca inicia OCR.

## Extracción y layout

El pipeline intenta texto nativo con geometría y lo clasifica como `RELIABLE`,
`SUSPECT` o `UNUSABLE`. Un resultado fiable evita Tesseract; uno sospechoso
reconcilia candidatos nativos y OCR; uno inutilizable usa OCR. La firma incluye
fuente, página, DPI, idiomas, Tesseract, OEM, PSM, preprocesamiento, parser,
agrupador y clasificador.

`PdfDocumentLayoutAnalyzer` resuelve columnas y orden, conserva overrides,
detecta headers y footers repetidos, y clasifica de forma conservadora tablas,
matemáticas y código. Todo contenido permanece persistido y buscable.

## Selección y consumidores directos

`DocumentSelectionRef` unifica:

- `BlockSelectionRef`;
- `PdfRegionSelectionRef`.

`ResolveDocumentSelectionUseCase` entrega `DocumentSelectionSnapshot` con
texto, procedencia, tipo, página, geometría, revisión y narratabilidad. El visor
mantiene una única selección activa y la invalida al cambiar de proyecto o al
reprocesar una región desaparecida.

Los siguientes consumidores consultan V2 directamente:

- proyección visual, hit testing y resaltado;
- outline/headings e índice;
- búsqueda sobre páginas preparadas;
- contexto alrededor de una región;
- actualización y restauración de overrides;
- narración mediante `BuildPreparedPdfNarrationUseCase`;
- localización inversa desde audio hacia página y región;
- diagnóstico de manifest, páginas, temporales, caché, revisiones y jobs.

El constructor genérico de narración rechaza PDF y atiende solo documentos por
bloques. No se deduce página, preparación ni narratabilidad desde metadatos de
`DocumentBlock`.

## Corpus y observabilidad

`PdfV2CorpusEndToEndTest` genera fixtures reproducibles con PDFBox: digital,
escaneado, híbrido, rotado, multicolumna, tabla, fórmula y OCR defectuoso. El
pipeline verifica importación, preparación, guardado/reapertura, búsqueda,
selección, narración y diagnóstico.

Los overlays PNG y el reporte JSON informativo se escriben en
`target/pdf-v2-corpus`. El reporte incluye tiempo, CPU, heap, almacenamiento,
archivos, tasa OCR y regiones dudosas; no impone todavía límites artificiales
de rendimiento.

## Análisis local especializado (esquema 3)

El esquema 3 añade `PdfPageAnalysisProfile`, idioma efectivo, rol de página,
perfil de preparación y tratamientos derivados revisables. Un derivado conserva
estado (`DRAFT`, `APPROVED`, `REJECTED` o `STALE`), revisión y huella de sus
regiones fuente, modelo, confianza, prompt y evidencia estructurada. Un cambio
manual en una región vuelve obsoletos solamente sus derivados dependientes.

La IA local no forma parte del subsistema PDF. Vive en `studio-media-api` bajo
los contratos transversales:

- `ContentAnalysisEngine`;
- `ContentAnalysisRequest`;
- `ContentAnalysisResult`;
- `ContentAnalysisEngineRegistry`;
- `MediaCapabilityService.analyzeContent`.

Estos contratos solo conocen texto, imágenes, roles y opciones neutrales. PDF
es un cliente que renderiza el ROI y reúne el contexto; teatro, el probador de
imágenes y futuros módulos pueden consumir exactamente los mismos motores. Una
prueba arquitectónica impide que los adaptadores locales importen tipos PDF.

Los motores registrados actualmente son:

- PP-StructureV3 para layout y reconocimiento matemático avanzado, únicamente
  en perfil `ENHANCED` o por petición explícita;
- Qwen3-VL 4B para descripción visual y corrección contextual;
- Qwen3-VL 4B para clasificación conservadora de narratabilidad por región,
  mediante una capacidad neutral separada;
- MathCAT para convertir MathML validado en habla matemática española o
  inglesa, sin red;
- el tratamiento determinista de tablas pequeñas.

PP-Structure procesa el PNG renderizado mediante un paquete local importado.
Paddle no puede iniciar descargas internas. Qwen usa un Ollama standalone
administrado: proceso oculto, loopback privado, puerto efímero, modelos y logs
dentro de `tools/document-ai`, PID propio y cierre de todo el árbol al terminar
la aplicación. Q8 es la elección automática con al menos 12 GB de RAM; Q4 es el
perfil económico. Preparar o volver a descargar siempre pasa por preflight.

La preferencia Q4/Q8 seleccionada manualmente se guarda en el almacén
administrado del motor y prevalece sobre la selección automática por RAM.
La corrección contextual se limita a la región y sus vecinas. Antes de guardar
una propuesta se verifica que sobrevivan cifras, unidades, nombres y operadores.
Toda propuesta permanece `DRAFT` hasta aprobación.

### Revisión semántica de narratabilidad

El JSON de página conserva siempre cada región descubierta. La revisión
semántica no es una segunda extracción ni un editor de OCR: recibe una
instantánea estructurada con IDs estables y propone una decisión de
narratabilidad para cada región. Su salida se persiste como
`PdfDerivedTreatmentKind.NARRATABILITY_REVIEW`.

```text
texto y evidencias canónicas de la página
→ solicitud neutral NARRATABILITY_CLASSIFICATION
→ borrador con decisión, razón y confianza por regionId
→ revisión humana
→ overrides de narratabilidad, sin borrar texto
```

La búsqueda y la revisión visual continúan mostrando regiones
`NON_NARRATABLE`; únicamente la proyección narrable las excluye. Si cambia el
texto, el orden o la revisión de una región, la huella invalida el derivado y
Qwen deberá analizar de nuevo esa página cuando el usuario lo solicite.

El reconocimiento de fórmulas produce LaTeX y usa un conversor determinista
conservador para obtener MathML. Las construcciones fuera del subconjunto
validado no se narran. MathCAT transforma el MathML seguro en texto hablado y
el usuario debe aprobar el derivado antes de que forme parte de la narración.

## Narración y foco no textual

La precedencia final es:

```text
override manual
→ derivado aprobado, vigente y con la misma revisión fuente
→ texto automático NARRATABLE
→ exclusión
```

Cada cue no textual incorpora `PdfNarrationFocusRef` en su procedencia de audio.
El visor navega a la página y dibuja uno o más rectángulos con borde grueso,
patrón y etiqueta accesible «Zona narrada». El foco permanece al pausar, cambia
sin parpadeo al hacer seek y desaparece al detener, cerrar o cambiar de cue. La
huella del derivado forma parte de `AudioSourceFingerprint`, por lo que solo se
invalidan sus chunks.

## Tablas académicas y derivados avanzados

`PdfTableStructure` es un derivado revisable con celdas estables, spans,
geometría, encabezados, caption, unidades y clasificación semántica. No es una
segunda fuente documental y nunca sustituye regiones V2.

La política de lectura es:

```text
prosa o clave/valor pequeña → lectura determinista por celdas
tabla mixta o numérica      → estructura + estadísticas verificadas
matriz o fórmula            → flujo matemático
contexto insuficiente       → anuncio de estructura
tabla grande                → resumen o rango explícito
```

Mínimos, máximos, variaciones y monotonicidad se calculan sin IA. Qwen solo
verbaliza cifras preexistentes y debe citar IDs de celda válidos. Una cifra,
unidad o celda inventada rechaza el derivado completo. La edición de celdas
afecta únicamente el derivado y conserva los IDs posicionales.

PP-Structure recibe el ROI renderizado mediante la operación neutral
`TABLE_STRUCTURE_RECOGNITION`. Su salida queda `DRAFT`, conserva el JSON
estructurado como evidencia y declara `canonicalTextModified=false`.

## Cola transversal de cómputo

`PriorityResourceScheduler` coordina voz y análisis sin que PDF conozca
motores concretos. Cada admisión declara prioridad, carga y demanda atómica de
CPU, GPU y memoria de modelo. Ningún trabajo retiene un recurso mientras
espera otro.

La cesión ocurre únicamente entre chunks o solicitudes. XTTS puede conservar
el modelo dentro de su concesión, pero lo descarga antes de ceder. Qwen
confirma la descarga con `/api/ps`. Por defecto solo puede residir un modelo
pesado; ampliar la capacidad requiere certificación concurrente con margen de
RAM y VRAM.

La cola es observable y cancelable. Cancelar un trabajo activo solicita el
siguiente límite seguro; no interrumpe una inferencia a mitad de escritura.

## Entrada compacta a Qwen

Los motores no reciben `PreparedPdfPage` ni su JSON. Cada operación construye
la mínima evidencia necesaria:

- galimatías: regiones textuales compactas e IDs;
- corrección: selección y hasta tres vecinas;
- tabla: celdas derivadas, estadísticas y contexto cercano;
- imagen o fórmula: ROI y contexto visual/textual cercano.

Si una página excede la ventana, se divide en grupos con heading, caption y
resumen común. El derivado se publica únicamente cuando todos los grupos son
válidos.

## Corpus real

`docs/product/pdf-real-corpus.json` registra sin copiar los libros Kress y
Burden del Escritorio y `De_los_subconjuntos_a_la_poda.pdf` de Descargas, con
ruta, páginas, SHA-256 y páginas anotadas. El harness opt-in
`PdfRealBooksCorpusTest` verifica la identidad de los tres originales,
renderiza las páginas seleccionadas y escribe overlays y resultados en
`target/pdf-real-corpus`.

La primera ejecución real detectó una regresión que los fixtures no mostraban:
el capturador común retenía solo los últimos 12.000 caracteres del XHTML
`pdftotext -bbox-layout`, eliminando el prólogo. El extractor nativo ahora usa
un temporal completo y lo elimina tras parsearlo; el PDF fuente nunca se toca.

`PdfRealBooksFullPreparationTest` es la puerta local larga para las 1.237
páginas. Referencia ambos originales en su ubicación, usa el pipeline canónico
native-first y prepara secuencialmente una sola página. Su reporte incremental
permite distinguir páginas OCR y regiones dudosas aunque una ejecución extensa
sea interrumpida.

## Recuperación y límites

- Un manifest incoherente o un hash de fuente distinto invalida el workspace.
- Temporales incompletos nunca sustituyen una página publicada.
- Un PDF fuente nunca se modifica.
- Solo existe un OCR local simultáneo.
- No se usa ningún servicio remoto silencioso.
- Los tratamientos de tabla deterministas permanecen disponibles.
- PP-Structure y Qwen requieren preparación explícita desde Motores y
  dependencias; abrir una página nunca los descarga ni ejecuta.
- El recorrido completo de 1.237 páginas y los smokes Qwen/PP-Structure se
  ejecutan únicamente como puertas locales explícitas, una vez preparados sus
  paquetes; nunca al abrir un documento.
