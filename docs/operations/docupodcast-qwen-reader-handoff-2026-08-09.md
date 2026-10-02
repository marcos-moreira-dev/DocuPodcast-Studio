# DocuPodcast Studio — handoff del lector Qwen (2026-08-09)

## 1. Objetivo y estado

DocuPodcast Studio transforma Word y PDF en lectura continua, audio sincronizado y proyección audiovisual. El lector PDF serial V1 quedó cerrado con Structured Output, validación Java de cobertura, un único pase de verificación materialmente distinto y publicación atómica.

El modelado físico de recursos ya quedó integrado en la única autoridad global. No se avanzó a concurrencia Qwen, TTS incremental ni FAST_LISTEN incremental. La siguiente fase exacta es endurecer thread safety del runtime Qwen antes de considerar capacidad 2.

## 2. Autoridades arquitectónicas

- PDF canónico: `PreparedPdfPage` y `PdfRegion`.
- Word canónico: `ReadableDocument` y `DocumentBlock`.
- Audio: `NarrationSegment`.
- Audiovisual: `DocumentContentItem` y `DocumentContentProjection`.
- Geometría/raster PDF: PDFBox.
- Semántica visual PDF: Qwen.
- Recursos: `MediaCapabilityService` y la única instancia global de `PriorityResourceScheduler`.

PDF no se convierte a `DocumentBlock`. El adaptador sintético antiguo sigue siendo compatibilidad, no modelo objetivo. OCR/Tesseract/PP-Structure permanecen como fallback o diagnóstico; el happy path no es OCR-first.

## 3. Flujo serial productivo

1. PDFBox rasteriza una página.
2. Cada página abre un request Qwen independiente, sin historial/KV de páginas anteriores.
3. Qwen devuelve un envelope JSON Schema mínimo.
4. Java hace parsing estricto, validación de tipos/bbox y normalización.
5. PDFBox aporta texto nativo solo como evidencia de cobertura cuando es fiable.
6. `PdfSemanticCoverageValidator` clasifica `ACCEPTED`, `NEEDS_VERIFICATION` o `REJECTED` y registra razones observables.
7. Si hace falta, se ejecuta una sola operación distinta de `COVERAGE_VERIFICATION/MISSING_REGION_DETECTION`.
8. Java valida, fusiona y deduplica determinísticamente, vuelve a comprobar cobertura y solo entonces publica.
9. `PreparedPdfPage` válida converge en persistencia, narración, resaltado y audiovisual.

Un intento inválido, truncado, incompleto, cancelado o todavía insuficiente no reemplaza una página canónica anterior. El output crudo se conserva en diagnósticos cuando la infraestructura lo permite.

## 4. Structured Output

El experimento inicial obtuvo 4/4 de sintaxis válida y solo 2/4 de semántica aceptable. Por eso Structured Output se mantuvo como framing, rodeado por validación de cobertura.

Envelope V1:

- raíz: `language`, `pageRole`, `regions`;
- región: `type`, `bbox`, `source`, `speech`;
- sin IDs, page number, readingOrder, revisiones, fingerprints ni estado persistente;
- sin `confidence` del modelo.

Qwen no genera el JSON canónico. Java sigue siendo responsable de parsing, IDs, orden, dominio y persistencia. `source` puede contener JSON, código, llaves, corchetes, fórmulas y Unicode ordinario.

## 5. Texto PDFBox como evidencia

`PdfBoxNativeTextEvidenceExtractor` usa PDFBox para obtener texto y bbox nativos. `PdfNativeTextQualityAssessor`, ya existente, decide conservadoramente si la capa es `RELIABLE`, `SUSPECT` o `UNUSABLE` según cantidad de caracteres/palabras, caracteres inválidos, fragmentación, solapes y score observable.

PDFBox no decide regiones, reading order final, figuras, tablas, ecuaciones, narración ni tipos. Solo responde si existe texto fiable que debería aparecer de alguna forma en la lectura semántica.

## 6. CoverageValidator

La comparación no es un diff literal. Normaliza Unicode, diacríticos, whitespace, puntuación y guiones de final de línea. Usa recall multiset de tokens significativos:

- cobertura global mínima: 0,84;
- cobertura por bloque largo: 0,58;
- cobertura por bloque corto significativo: 0,68;
- excluye page numbers y headers/footers breves en extremos;
- detecta títulos, párrafos largos y spans significativos omitidos.

Si PDFBox fragmenta una capa fiable en trozos de uno o dos tokens y no existe ningún span individual evaluable, usa recall agregado de toda la evidencia útil. No interpreta `0/0` como cobertura cero.

También valida coherencia observable:

- TABLE debe demostrar filas/celdas mediante separadores o filas/celdas etiquetadas;
- IMAGE debe tener explicación visual en `speech`;
- MATH largo sin señales matemáticas es sospechoso;
- roles `INDEX`, `CATALOG` o `VISUAL_REFERENCE` incompatibles con mucha prosa explicativa son sospechosos.

Java no reetiqueta silenciosamente. Una incoherencia activa verificación o rechazo.

## 7. Segundo pase

Existe como máximo un segundo pase automático y no repite la petición primaria. Recibe:

- la misma imagen;
- resumen compacto de regiones detectadas;
- razones de validación traducidas a instrucciones accionables;
- evidencia textual posiblemente ausente cuando existe.

Devuelve `pageRole` y solo regiones faltantes/correcciones. Java fusiona por texto normalizado o solape bbox, evita duplicados, conserva la corrección más informativa, reordena por geometría y repite el gate.

No hay bucles ni retry idéntico por output agotado. Transporte/backend transitorio, timeout/stall, OOM, truncación, protocolo inválido y cancelación permanecen categorías distintas.

## 8. Páginas sin texto nativo fiable

Qwen sigue siendo el lector primario. La ausencia de evidencia obliga a un segundo pase visual distinto. Si después del verificador persiste una incoherencia observable, la página se rechaza/fallback; nunca se publica silenciosamente. OCR puede intervenir después como fallback compatible, no como autoridad ordinaria.

## 9. Runtime probado

- Modelo: `qwen3-vl:4b-instruct-q8_0`, 4,4B, Q8_0, ~5,85 GB.
- Ollama empaquetado 0.32.5.
- `num_ctx=8192`, batch 512, Flash Attention ON, KV cache `q8_0`.
- paralelismo efectivo 1, GPU-first con RAM/CPU offload.
- GTX 1650 4 GB, 32 GB RAM.
- VRAM efectiva observada: ~1,87 GB en Ollama AUTO.

8K/Q8/Flash es un perfil probado para este hardware, no una regla universal. Cada página usa contexto limpio; los pesos pueden permanecer residentes durante primary+verifier mediante el batch del servicio.

## 10. Corpus físico final

| Caso | Primario | Verificador | Resultado del gate |
|---|---:|---:|---|
| Tabla/prosa (`informe-page-02`) | 892,9 s; 1.606 tokens; 2,149 tok/s | 101,1 s; 16 tokens | Aceptado. Conserva prosa y todas las celdas; corrige `INDEX` a `CONTENT`; cobertura PDFBox real aprobada. |
| Matemática/figura (`demostracion-limite/page-001`) | 375,7 s; 799 tokens; 2,900 tok/s | 374,4 s; 867 tokens | Aceptado. Conserva prosa/fórmula; corrige `CATALOG`; añade rótulos y relaciones de la figura; cobertura PDFBox real aprobada. |
| Código/prosa (`de-los-subconjuntos/page-0082`) | 647,4 s; 1.304 tokens; 2,210 tok/s | 78,1 s; 16 tokens | Aceptado sin native text: algoritmo de 7 líneas, fórmulas, títulos, teorema y demostración presentes; verificador devuelve `CONTENT` y cero faltantes. |

Los tres primarios terminaron con `doneReason=stop`, envelope completo, bbox válidos, Q8/8K/Flash y sin `confidence`. El replay físico opcional pasó los envelopes reales por parser estricto, evidencia PDFBox y `CoverageValidator`.

La latencia sigue siendo deuda: páginas densas tardan aproximadamente 6–15 minutos en ejecución fría/aislada. El cuello dominante observado es la decodificación a ~2–3 tok/s, no la ventana 8K.

## 11. Falsos positivos/negativos encontrados

- Falso positivo corregido: TABLE completa expresada como filas etiquetadas se marcaba incompleta por no usar `;`.
- Falso rechazo corregido: capa PDFBox fiable fragmentada en tokens cortos producía cobertura 0/0.
- Error real detectado: Qwen asignó `INDEX`/`CATALOG` a páginas explicativas; ahora activa verificación y no se corrige en Java.
- Error real detectado: IMAGE sin `speech`; bloquea publicación hasta una corrección observable.
- Obediencia imperfecta: el verificador matemático repitió regiones primarias además de corregir IMAGE. La fusión determinista deduplica; queda como deuda de prompt/telemetría, no pérdida de contenido.

## 12. Persistencia, narración y highlight

Las pruebas integradas confirman que una `PreparedPdfPage` aceptada se persiste y reabre, genera `NarrationSegment` con binding canónico y resuelve resaltado sobre el mismo mapa de página. La publicación atómica conserva páginas anteriores ante error de protocolo o cobertura.

## 13. Regresión Word

Word permaneció canónicamente separado. Pasaron importación DOCX básica/avanzada/diagnóstica, narratabilidad, bloques visuales, imágenes semánticas, materialización, guion, asignación efectiva de voz, plan de audio y playback. No se alteró el modelo Word para acomodar PDF.

## 14. Tests y smokes

Clases nuevas/relevantes:

- `StructuredPdfSemanticPageResponseParserTest`;
- `PdfSemanticCoverageValidatorTest`;
- `PdfBoxNativeTextEvidenceExtractorTest`;
- `AnalyzePdfPageSemanticallyUseCaseTest`;
- `PdfSemanticPhysicalReplayTest` (opcional, corpus físico local);
- `QwenPageProtocolRealSmokeTest` (opcional, primary y verifier reales);
- persistencia/narración/highlight: `JsonPreparedPdfDocumentRepositoryTest`, `PdfV2CorpusEndToEndTest`, `BuildPreparedPdfNarrationDerivedFocusTest`, `ResolvePdfPlaybackHighlightUseCaseTest`;
- regresión DOCX/Word y audio relacionada.

La suite completa final aprobó 1.219 tests, 0 fallos, 0 errores y 13 omitidos. Los omitidos incluyen los smokes físicos explícitos, que se ejecutaron por separado con propiedades locales y quedaron verdes.

## 15. Modelo físico de recursos cerrado

La única autoridad productiva sigue siendo la instancia de `PriorityResourceScheduler` creada en `ApplicationBootstrap` y usada por `MediaCapabilityService`. `GenerationJobService` ya no puede fabricar implícitamente un scheduler local: su composición exige inyección. `LocalResourceScheduler` queda como adaptador de compatibilidad y pruebas, sin instancia productiva.

El modelo anterior de unidades se evolucionó, sin crear una autoridad paralela, con:

- `ComputeDeviceId`: identidad CPU/GPU con proveedor e índice, sin hardcode NVIDIA;
- `ComputeResourceBudget` y `ComputeDeviceBudget`: capacidad física declarada separada de presupuesto de aplicación, RAM host, VRAM por dispositivo, compute abstracto y encoders;
- `ComputeResourceTelemetry`: observación best-effort separada de la admisión;
- `ModelResidencyKey`/`ModelResidencyDemand`: backend, modelo, perfil runtime y placement;
- `ComputeResourceDemand`: coste marginal host/VRAM, offload, compute, residencia y encoder exacto;
- `EncoderResourceDemand`/`VideoEncoderKind`: encoder físico ligado al dispositivo;
- snapshots con presupuesto, reservas host/device, residencias, consumidores activos y cola.

La admisión sigue siendo atómica: no reserva parcialmente CPU, RAM, VRAM, compute ni encoder. Una residencia confirmada se cobra una sola vez, permanece tras cerrar el último request mientras el runtime siga cargado y solo se libera al confirmar unload real. La cancelación libera el coste marginal sin inventar una descarga de pesos.

Los defaults continúan conservadores: `QWEN_INFERENCE=1`. El perfil PAGE probado describe Qwen 4B Q8, contexto 8K, KV q8_0, Flash, batch 512, GPU compute y offload host, pero 8K no se convirtió en regla universal. Piper CPU, XTTS CPU/GPU y render pueden expresarse sin compartir falsamente un único `MODEL_MEMORY`.

## 16. Encoder y selección de dispositivo

`OperationalSettings` existente sigue siendo la intención del usuario. No se creó otro árbol de settings. La ruta conserva `CPU_X264`, `NVIDIA_NVENC`, `INTEL_QSV` y `AMD_AMF` hasta `VideoEncodingPreference` y el adaptador FFmpeg. Los codecs finales son respectivamente `libx264`, `h264_nvenc`, `h264_qsv` y `h264_amf`.

`selectedDeviceId` se conserva desde exportación hasta las opciones de `VideoRenderRequest`; `MediaCapabilityService` asocia la demanda de encoder/VRAM/compute al dispositivo concreto. Queda pendiente traducir el índice de dispositivo a flags FFmpeg específicos de cada proveedor cuando se habilite soporte multi-GPU productivo; no se inventaron flags universales incorrectos.

## 17. Lifecycle Qwen endurecido

`OllamaModelLifecycle` es ahora la autoridad corta y sincronizada del estado físico real del runtime. No hace HTTP ni admisión de recursos. Separa generación del proceso, modelos residentes, owners de residencia y requests activos. `ManagedOllamaProcess.ModelRequest` representa una inferencia concreta con snapshot inmutable de `ModelResidencyKey`, compute preference, request ID y generación del runtime.

La inferencia HTTP continúa fuera del monitor. Antes de enviar se registra globalmente el request y el decremento ocurre en `close()` incluso ante cancelación, timeout, protocolo inválido o excepción. Un unload solicitado durante actividad queda pendiente. Solo el último request, sin owner de batch compatible, puede iniciar el unload físico; requests nuevos del mismo modelo esperan mientras el unload está en curso.

Un cambio GPU/CPU/device no reinicia el runtime bajo requests activos. Se registra como preference pendiente, los requests existentes conservan su snapshot y el siguiente espera al safe point antes del restart. Si el proceso muere, aumenta la generación, se invalida toda residencia real y cada request activo obtiene `CHILD_EXIT` con `failureScope=RUNTIME_FAILURE`. Errores HTTP, timeout, protocolo y OOM siguen siendo fallos de request y no descargan el modelo de otro consumidor.

## 18. Batch, cancelación y shutdown

`QwenVisualAnalysisEngine` eliminó `ThreadLocal` como autoridad de batch/residencia. Los batches tienen owners explícitos globales y nesting por owner. Un owner puente protege la carrera `endBatch` frente a una request que está entrando: cerrar el batch retira ownership, pero el unload se difiere si existe request/intención activa. Batch comparte pesos, nunca prompt, KV, imagen, response buffer ni conversación.

Cancelar o fallar A solo cierra la lease/request A. B conserva snapshot, contexto y modelo. La cancelación de streaming cierra su InputStream/future; Ollama 0.32.5 todavía no está verificado físicamente para cancelación HTTP independiente de dos inferencias. `close()` de aplicación bloquea nuevas adquisiciones, detiene el backend e invalida residencia; el stop administrativo normal no se reutiliza como cleanup ordinario de página.

## 19. Tests y readiness

Tests controlados cubren A/B activos, cancelación/finalización independiente, fallo/runtime death, endBatch con request activo, unload tras último consumidor, cambio de preference, misma/diferente identidad, shutdown y carreras finish+unload, batch-close+start y death+finish. Se conservan además residencia del scheduler, presupuestos, PAGE, CoverageValidator, persistencia, narración, highlight, Word y encoder.

Matriz actual:

- Java lifecycle thread-safe: **YES** para requests independientes y lifecycle del runtime probado con fakes.
- Backend Ollama capaz de dos requests independientes: **UNVERIFIED**; producción mantiene `OLLAMA_NUM_PARALLEL=1`.
- Model residency sharing: **PARTIAL**; verificado en Java/scheduler, todavía no en dos requests físicos Ollama.
- Cancellation isolation: **VERIFIED en Java / LIMITED BY BACKEND** hasta prueba física.
- Seguro ejecutar experimento físico concurrency=2: **YES**, únicamente con perfil experimental aislado y sin cambiar defaults; su ejecución está bloqueada ahora por la imagen de corpus ausente.

Deuda aplazada: prueba física backend 2, telemetría/calibración, multi-GPU/flags FFmpeg, TTS/FAST_LISTEN incremental y rendimiento. `QWEN_INFERENCE=1` permanece inalterado.

# Chat handoff — copiar a una nueva conversación

DocuPodcast Studio mantiene Word (`ReadableDocument`/`DocumentBlock`) y PDF (`PreparedPdfPage`/`PdfRegion`) canónicamente separados. El lector PDF serial V1, CoverageValidator, segundo pase y publicación atómica están cerrados. El modelo físico también está cerrado dentro de la única `PriorityResourceScheduler` global y `MediaCapabilityService`; no crees schedulers paralelos.

El lifecycle Java Qwen ya fue endurecido. `OllamaModelLifecycle` separa runtime, residencia y requests; `ManagedOllamaProcess.ModelRequest` registra cada request con snapshot inmutable de modelo, placement/preference y generación. Requests activos y owners de batch son contadores globales sincronizados mediante secciones críticas cortas; la inferencia HTTP nunca mantiene esos monitores. Unload queda pendiente hasta no tener requests ni owners, la residencia solo se libera tras confirmación física y runtime death invalida todas las sesiones con `CHILD_EXIT/RUNTIME_FAILURE`.

`QwenVisualAnalysisEngine` ya no usa ThreadLocal para residencia/batch. Los batches tienen ownership explícito y un bridge owner cierra la carrera endBatch/request-start. Cancelar o fallar A no descarga ni cancela B. Cada request conserva prompt, imagen, KV/contexto, response/progress y compute snapshot propios. Cambiar GPU/CPU/device se difiere hasta un safe point; no hay hot migration.

Los tests con dos requests simulados y carreras están verdes, pero el backend Ollama 0.32.5 no ha sido probado con dos inferencias físicas independientes. Producción continúa con `QWEN_INFERENCE=1`, `OLLAMA_NUM_PARALLEL=1` y `OLLAMA_MAX_LOADED_MODELS=1`. No eleves esos defaults. La próxima fase puede preparar —sin activar por defecto— un perfil experimental concurrency 2, restaurar legítimamente la imagen/corpus de smoke y medir corrección, cancelación, pages/minute, VRAM y RAM. Si el corpus sigue ausente, detente y reporta la omisión.

## 20. Experimento físico concurrency=2 (2026-08-09)

### Corpus y perfil

`target/pdf-real-corpus/burden/page-0895-overlay.png` no existe en Git, LFS,
historial, fixtures ni un pipeline reproducible conocido. No se fabricó ni se
sustituyó silenciosamente. Se usaron dos páginas reales ya documentadas:

- A: `tmp/pdfs/informe-page-02.png`, tabla/prosa, 326.094 bytes;
- B: `tmp/pdfs/demostracion-limite/page-001.png`, matemática/figura, 133.849 bytes.

El perfil solo se activa con
`docupodcast.qwen.experimentalConcurrency.enabled=true`; limita parallel a 2 y
mantiene `OLLAMA_MAX_LOADED_MODELS=1`. Modelo/request: Qwen3-VL 4,4B Q8_0,
contexto 8192, KV q8_0, Flash Attention, batch 512, GPU-first con host offload y
salida máxima 1800. Los defaults productivos permanecen en 1.

Una primera corrida se abortó antes de medir al detectar un `llama-server`
huérfano que ocupaba 941 MiB de VRAM. Se cerró solo ese proceso, se comprobó
VRAM 0 MiB y se reinició el experimento aislado.

### Baseline y perfil 2 solicitado

| Escenario/página | Latencia request | Prompt eval | Generación | Tokens | Decode |
|---|---:|---:|---:|---:|---:|
| Serial A | 863.293 ms | 40.421 ms | 788.583 ms | 1.734 | 2,199 tok/s |
| Serial B | 468.352 ms | 23.619 ms | 443.914 ms | 1.148 | 2,586 tok/s |
| Perfil 2, B atendida primero | 462.960 ms | 24.182 ms | 419.158 ms | 1.148 | 2,739 tok/s |
| Perfil 2, A incluida espera | 1.312.970 ms | 40.006 ms | 809.730 ms | 1.734 | 2,141 tok/s |

Serial A+B: 1.332.660 ms (22 min 12,660 s), 0,090045 páginas/minuto.
Perfil 2 solicitado: 1.313.965 ms (21 min 53,965 s), 0,091327 páginas/minuto;
cociente 1,0142. La diferencia de +1,42 % es ruido/orden, no paralelismo.

Aunque Java mantuvo dos requests activos y dos HTTP solapados durante 455.776 ms,
Ollama registró `model architecture does not currently support parallel requests`
para `architecture=qwen3vl` y lanzó `llama-server -np 1`. Solo existió `slot id 0`:
B terminó y después comenzó A. Clasificación final: **D. BACKEND SERIALIZES**.

### Memoria, residencia y scheduler

- VRAM pico serial y perfil 2: 2.981 MiB en ambos.
- `/api/ps`: una residencia de 5.849.187.611 bytes totales y
  1.872.211.475 bytes efectivos en VRAM.
- llama.cpp: 9/37 capas, model buffer CUDA 1.212,42 MiB y host 3.258,13 MiB.
- RAM total del sistema pico: 24.265.728.000 bytes serial y 25.803.579.392 bytes
  con dos requests admitidos. Incluye procesos ajenos; no implica dos pesos.
- CPU total alcanzó 100 % en ambos por el offload host.
- Snapshot: tres leases (batch + A + B), 5.268.045.824 bytes host y
  2.692.743.168 bytes VRAM reservados. Una sola residencia: host
  4.194.304.000, VRAM 1.887.436.800, tres consumidores. No hubo doble cobro.
- `runtimeConfirmed=false` en el snapshot temprano, antes de terminar la carga;
  los diagnósticos posteriores confirmaron `modelLoaded=true`.
- Una carga física por escenario aislado, nunca dos dentro de A+B.

### Aislamiento, cancelación y unload

SHA-256 A serial/concurrente, idéntico:
`DC98CD5815D36E9FCC9B7FFCA1738303F194D180DC0237A675A351029AD54F49`.
SHA-256 B serial/concurrente/cancelación, idéntico:
`D2A25B2E294DCEE40BDBC9DB9E6ED8B0D56F0ECC7DC04A7B35BF0BB3EFCB02D1`.
Los anchors exclusivos tampoco aparecen cruzados: no hubo cross-talk.

En cancelación B obtuvo el único slot y A quedó en cola. B terminó válida en
472.489 ms; A reconoció cancelación a los 512.522 ms, después de que B liberó el
slot y A inició prefill. No mató ni degradó B, pero no es inmediata mientras
`HttpClient.send` espera cabeceras. Tras el último consumidor se ejecutó unload,
`/api/ps` quedó vacío y VRAM regresó a 0 MiB.

### Gate semántico y bug real corregido

Sintaxis/parser/bbox fueron válidos y no hubo cross-talk, pero A resumió la
TABLE como `Tabla de hallazgos en una página`, perdió las celdas y usó `INDEX`.
B usó `BIBLIOGRAPHY`, aunque conservó prosa, fórmula e IMAGE con speech.

El pase distinto de B devolvió `CONTENT` y cero faltantes; el replay Java lo
acepta con `CoverageValidator`. A no es publicable: el verificador completo
agotó 1.200 tokens y una recuperación materialmente distinta sobre el ROI real
de la tabla agotó 1.400. No hubo retry idéntico ni tercer intento.
`CoverageValidator` rechaza A con `tableMissingVisibleCells`; no se publicó una
`PreparedPdfPage` parcial.

El primer truncado reveló un bug real: una respuesta JSON incompleta se
clasificaba `INVALID_OUTPUT` antes de inspeccionar `done_reason=length`. Se
corrigió el orden; la recuperación ROI quedó correctamente diagnosticada como
`OUTPUT_TRUNCATED` y conserva raw output en diagnósticos.

### Decisión

No se certifica concurrency 2 productiva. En Ollama 0.32.5 Qwen3-VL fuerza
`np=1`; quedan `QWEN_INFERENCE=1`, `OLLAMA_NUM_PARALLEL=1` y
`OLLAMA_MAX_LOADED_MODELS=1`. No se probó 3 ni se cambió AUTO/settings.

Una versión futura con soporte paralelo `qwen3vl` deberá repetir la certificación.
Con el runtime actual, la siguiente optimización útil es mantener Qwen serial y
preparar página aceptada → `NarrationSegment` → TTS mientras continúa el lector.
Queda como deuda separada recuperar TABLE densas sin resumir ni truncar.

Artefactos: `target/qwen-concurrency2-experiment/20260809-154104`.

Validación final: `mvn test` aprobó 1.246 tests, 0 fallos, 0 errores y
16 omitidos (smokes físicos opt-in incluidos). El replay físico explícito pasó
parser/aislamiento y los estados A rechazado/B aceptado esperados. Regresiones
Word, narración, highlight, lifecycle, scheduler, PAGE, cobertura, publicación
atómica y encoder permanecieron verdes. `git diff --check` terminó con código 0;
solo informó avisos de conversión LF/CRLF del working tree preexistente.

# Chat handoff — experimento físico concurrency=2

El experimento 1-vs-2 se ejecutó con dos páginas reales, Qwen3-VL 4,4B Q8, 8K,
KV q8_0, Flash y batch 512. El asset burden no era restaurable y no se inventó.
Baseline: 0,090045 páginas/min. Perfil 2 solicitado: 0,091327, pero Ollama 0.32.5
registró que qwen3vl no soporta requests paralelos y lanzó `-np 1`; B terminó
antes de iniciar A. Clasificación D: BACKEND SERIALIZES. Hubo una sola residencia,
sin doble cobro ni cross-talk. Cancelar A encolada no afectó B, pero solo fue
observado al alcanzar el slot/prefill. Unload final confirmado.

B pasó CoverageValidator tras corregir su rol. A fue rechazada porque resumió la
tabla y perdió celdas; el verificador completo y una recuperación distinta por
ROI truncaron, sin publicación parcial. Se corrigió la clasificación Java para
priorizar `done_reason=length` como OUTPUT_TRUNCATED incluso si el JSON parcial
no puede extraerse. Producción sigue 1/1/1. No pruebes 3 ni habilites AUTO=2.
Siguiente fase recomendada: TTS incremental sobre páginas aceptadas con Qwen
serial; recuperación de tablas densas queda como deuda explícita.

## 21. TTS incremental por página PDF (2026-08-09)

### Cuello y arquitectura elegida

El congelamiento ocurría en dos fronteras. `PreparePdfScopeUseCase` recibía el
resultado individual de cada página, pero solo publicaba progreso y devolvía el
resultado de alcance después de `CompletableFuture.allOf`. Después,
`BuildPreparedPdfNarrationUseCase` cargaba todas las páginas persistidas y
`AudioGenerationRequest` capturaba una lista cerrada de unidades. Las páginas
aceptadas durante ese job no podían incorporarse.

Se eligió la opción A: jobs TTS pequeños por página, producidos por un callback
directo de publicación canónica. No se creó event bus ni otro modelo de dominio.
El callback solo se emite para `PreparePdfPageResult.succeeded()`, es decir,
después de validación y publicación atómica de `PreparedPdfPage`.

`BuildPreparedPdfNarrationUseCase.buildPage` carga una sola página canónica y
genera los `NarrationSegment`/`PdfNarrationBinding` existentes. `mergePage`
reemplaza únicamente esa página, conserva la identidad del guion y ordena por
página. Los IDs `PDFSEG-*` no cambiaron. Una revisión real conserva el ID de
región/segmento, pero cambia el fingerprint vigente y por tanto invalida solo el
audio obsoleto mediante `ReusableAudioCoverage`.

### Ejecución, orden y backpressure

`PdfNarratablePreparationCoordinator` abre una ventana incremental para
`FAST_LISTEN` y `PROCESS_COMPLETE`, y entrega cada página aceptada al view-model.
El productor PDF puede continuar en su `PdfPagePreparationScheduler` serial
mientras la cola de voz existente sintetiza el batch de la página anterior.
Solo existe un job TTS incremental activo; el backlog explícito está acotado a
tres números de página. Si llegan más, no se acumulan buffers ni requests: al
liberarse TTS se reconstruye la cobertura desde el guion canónico y los jobs
persistidos.

La selección del siguiente batch se hace por página y orden de lectura, no por
orden de terminación. El manifest combinado continúa construyéndose con
`ReusableAudioCoverage`, por lo que WAVs de jobs distintos se reutilizan y se
ordenan según el guion. El highlight conserva
`NarrationSegment -> PdfNarrationBinding -> PdfRegion/bbox`; no se crean cajas
por frase.

`FAST_LISTEN` preserva página/región activa, habilita playback al aparecer el
primer WAV y mantiene el look-ahead activo hasta terminar su ventana. Una página
y audio ya cacheados evitan Qwen y TTS. `PROCESS_COMPLETE` recorre el alcance
total y produce TTS por página sin autoplay. Al terminar tratamientos semánticos
se reconcilia el guion canónico y solo se generan unidades nuevas o stale.

Cancelar desactiva el productor TTS y cancela el job activo mediante las APIs
existentes, sin borrar páginas ni WAVs ya cerrados. Un fallo TTS marca solo esa
página para la sesión y permite continuar; un fallo Qwen no emite callback y no
inventa audio. Piper y XTTS siguen entrando por `MediaCapabilityService` y la
única `PriorityResourceScheduler`: Piper CPU puede coexistir cuando la admisión
lo permita; XTTS GPU no tiene bypass ni solapamiento hardcodeado con Qwen.

### Progreso y time-to-first-audio

La UI distingue preparación PDF de síntesis concurrente en el mismo mensaje de
estado. Se instrumentaron T0 solicitud, T1 primera página aceptada, T2 primeros
segmentos, T3 primer WAV y T4 inicio de playback mediante
`incrementalPdfAudioTimingLabel()`.

La prueba integrada controlada mantiene la página 2 bloqueada en preparación y
confirma que el WAV de la página 1 aparece antes de completar el alcance. Antes,
TTS solo podía empezar tras el `allOf`; después, T3 ocurre con el future global
todavía incompleto. No se ejecutó un smoke físico Qwen+Piper/XTTS en esta fase,
por lo que no se reportan segundos artificiales; la telemetría T0-T4 queda lista
para medirlos en una ejecución real.

### Validación y deuda

Se añadieron pruebas para publicación antes del alcance completo, pipeline
página->segmentos->WAV, merge fuera de orden, IDs estables y reemplazo aislado
de una página. La regresión focal cubrió bindings/focus PDF, cobertura de audio,
gateways mock/real, arquitectura de flujos y DOCX. `mvn test` terminó con 1.250
tests, 0 fallos, 0 errores y 16 omitidos. `git diff --check` terminó con código
0; solo mostró avisos LF/CRLF preexistentes.

Qwen continúa estrictamente 1/1/1: `QWEN_INFERENCE=1`,
`OLLAMA_NUM_PARALLEL=1`, `OLLAMA_MAX_LOADED_MODELS=1`. No se reabrió concurrencia.
La recuperación de tablas densas sigue separada. Queda una fase adicional de
experiencia real FAST_LISTEN/navegación/prefetch para medir T0-T4 físico,
calibrar el tamaño de ventana y validar cancelación/navegación manual con
Piper/XTTS; no requiere cambiar el pipeline ni la concurrencia Qwen.

# Chat handoff — TTS incremental PDF

El lector PDF V1 publica cada `PreparedPdfPage` atómicamente y Qwen permanece
serial en Ollama 0.32.5: producción sigue `QWEN_INFERENCE=1`,
`OLLAMA_NUM_PARALLEL=1`, `OLLAMA_MAX_LOADED_MODELS=1`. El experimento físico dio
D, BACKEND SERIALIZES (`-np 1`, slot 0); no vuelvas a probar concurrency 2/3.

Ya está implementado el pipeline incremental: el callback de
`PreparePdfScopeUseCase` observa solo páginas canónicas aceptadas;
`BuildPreparedPdfNarrationUseCase.buildPage/mergePage` produce y agrega los
`NarrationSegment` existentes sin cambiar IDs; el view-model programa batches
TTS por página, deduplica con `ReusableAudioCoverage`, conserva orden de playback
y bindings/highlight, y permite que Qwen prepare la siguiente página. El backlog
explícito es 3 y el resto se recupera escaneando cobertura persistida, sin cola
infinita. FAST_LISTEN hace autoplay desde página/región activa; PROCESS_COMPLETE
no hace autoplay. Cache/reopen evita Qwen/TTS cuando los artefactos son vigentes.

Piper/XTTS siguen pasando por `MediaCapabilityService` y la única
`PriorityResourceScheduler`; no hay bypass ni supuesto fijo de coexistencia GPU.
La suite completa está verde: 1.250 tests, 0 fallos/errores, 16 omitidos. Una
prueba integrada demuestra primer WAV de P1 mientras P2 sigue en preparación.
No hubo smoke físico en esta fase. Siguiente fase exacta: validar experiencia
FAST_LISTEN/navegación/prefetch con un PDF real de 2-3 páginas, registrar T0-T4
físico con Piper y, si la admisión lo permite, XTTS; ajustar solo UX/ventana y
cerrar robustez. Mantén como deuda separada la recuperación de tablas densas.

## 22. Cierre físico del núcleo FAST_LISTEN (2026-08-09)

### Corpus, runtime y evidencia

El smoke opt-in utilizó `D:\Proyectos\Demostracion_limite_notable.pdf`, un PDF
real de tres páginas con prosa, fórmulas, diagrama, tabla sencilla y gráfica. Se
inspeccionaron visualmente las tres páginas. Qwen usó
`qwen3-vl:4b-instruct-q8_0`, contexto 8192 y ejecución serial; Piper fue el
backend TTS físico obligatorio.

La ejecución aceptada quedó en
`target/fast-listen-physical/FAST_LISTEN_PHYSICAL_REPORT.md`, con raw outputs y
eventos en el mismo directorio. Los tiempos medidos fueron:

- T0→T1, primera página canónica aceptada: 295.981 ms.
- T1→T2, segmentos disponibles: 0 ms.
- T2→T3, primer WAV Piper terminado: 5.425 ms.
- T3→T4, `Clip` físico abierto e iniciado: 603 ms.
- T0→T4, time-to-first-playback: 302.011 ms.

P2 estuvo en Qwen entre 295.982 y 746.309 ms, mientras Piper sintetizó P1 entre
296.035 y 301.407 ms. El solapamiento Qwen P2 + TTS P1 fue físico, no simulado.
El WAV es `target/fast-listen-physical/page-01-piper.wav`. P1 y P2 terminaron el
protocolo V1 en `DONE`; P2 preservó las celdas visibles de la tabla.

### Bugs físicos encontrados y corregidos

Producción todavía componía el parser JSON antiguo. Ahora
`WorkspaceCompositionFactory` usa exclusivamente
`BlockPdfSemanticPageResponseParser`; un guardrail arquitectónico impide la
regresión. `AnalyzePdfPageSemanticallyUseCase` ya solicita el contrato
`PAGE/BEGIN/SOURCE/SPEECH/END/DONE`, sin schema JSON, y el prompt contiene un
ejemplo completo que prohíbe placeholders y exige `SOURCE` y `DONE`.

El parser permite una respuesta de verificación completa sin regiones, pero el
pase primario sigue rechazando páginas vacías. Se normalizó el alias observado
`BOX` a `SIDEBAR`, sin ampliar el modelo canónico. Las respuestas físicas
incompletas o inválidas —placeholder, falta de `PAGE`, `SOURCE` o `DONE`— fueron
rechazadas y nunca reemplazaron una página válida.

La composición global tenía `CPU_HEAVY=1`, lo cual serializaba Qwen y Piper. El
único scheduler global ahora usa `incrementalReaderDefaults()`: mantiene
`QWEN_INFERENCE=1` y límites conservadores, pero admite dos trabajos CPU-heavy
para permitir un Qwen y un Piper. No se añadió otro scheduler ni se habilitó
concurrencia Qwen.

### Navegación, orden, cache y aislamiento

Las regresiones lógicas verifican el ancla de página/región activa, la ventana
actual→siguiente, repriorización sin destruir páginas aceptadas, dedupe y orden
del scheduler, publicación incremental, backlog acotado, orden de playback por
página/readingOrder/segmento, reuse por fingerprint, reapertura, invalidación
selectiva, highlight `NarrationSegment→PdfNarrationBinding→PdfRegion/bbox`,
fallos Qwen/TTS aislados y `PROCESS_COMPLETE` sin autoplay. Pause/resume actúan
solo sobre el transporte; stop limpia la intención/transporte, no jobs, WAV,
páginas ni residencia Qwen.

No se certificaron mediante clicks automáticos navegación P1/P2/P3, pause,
resume y stop dentro de JavaFX: la aplicación había sido cerrada y el conector
de control devolvió `node_repl exec context not found`. No se sustituyó esa
observación por tiempos inventados. El núcleo físico
solicitud→Qwen→página→segmentos→Piper→WAV→playback y su overlap sí quedan
certificados; la aceptación visual/manual de esos controles es el único gate
corto pendiente.

### Validación final y siguiente fase

La suite completa terminó verde con 1.256 pruebas, 0 fallos, 0 errores y 17
omitidas. La regresión Word focal —importación DOCX, bloques visuales,
narratabilidad, repositorio, `ReadableDocument`, guion y semántica Word— también
pasó. `git diff --check` terminó con código 0. Qwen permanece 1/1/1:
`QWEN_INFERENCE=1`, `OLLAMA_NUM_PARALLEL=1`,
`OLLAMA_MAX_LOADED_MODELS=1`.

La próxima fase de implementación es recuperación semántica de páginas difíciles:
tablas densas, multicolumna y contenido parcialmente omitido. Antes de declararla
iniciada, queda ejecutar el gate manual JavaFX de inicio desde P2 o región activa,
navegación a P3, pause/resume, stop y reapertura/cache. XTTS sigue siendo smoke
secundario opcional; Piper ya probó el flujo físico obligatorio.

# Chat handoff — copiar a una nueva conversación

DocuPodcast Studio tiene cerrado físicamente el núcleo incremental FAST_LISTEN
con `D:\Proyectos\Demostracion_limite_notable.pdf`. Qwen3-VL 4B Q8, contexto 8K
y Piper produjeron P1 canónica, segmentos, WAV y playback en 302.011 ms. Mientras
Piper sintetizaba P1 (296.035..301.407 ms), Qwen procesaba P2
(295.982..746.309 ms): overlap real confirmado. Evidencia completa en
`target/fast-listen-physical/FAST_LISTEN_PHYSICAL_REPORT.md`.

Se corrigieron cuatro fronteras reales: producción ahora usa el parser por
bloques V1; el prompt exige `PAGE/BEGIN/SOURCE/SPEECH/END/DONE`; el
parser/verificador distingue una verificación vacía válida de una primaria vacía
inválida y normaliza `BOX` a `SIDEBAR`; el único `PriorityResourceScheduler` usa
`incrementalReaderDefaults()` para permitir Qwen+Piper sin abrir Qwen paralelo.
Qwen sigue `QWEN_INFERENCE=1`, `OLLAMA_NUM_PARALLEL=1` y
`OLLAMA_MAX_LOADED_MODELS=1`.

La suite completa está verde: 1.256 pruebas, 0 fallos/errores, 17 omitidas;
regresión Word focal y `git diff --check` verdes. Orden, prefetch, anclas,
reapertura/cache, invalidación selectiva, highlight, fallos aislados,
pause/resume/stop no destructivos y PROCESS_COMPLETE sin autoplay están cubiertos
por regresión lógica. Falta solo el gate manual JavaFX de clicks reales porque el
conector falló con `node_repl exec context not found`: abrir el PDF, escuchar
desde P2/región, navegar a P3, pausar, reanudar, detener y reabrir para confirmar
el fast path visual. No inventes resultados de ese gate.

Después del gate manual, la siguiente fase es recuperación semántica de páginas
difíciles: tablas densas, multicolumna y omisiones parciales. No reabras
concurrency 2/3 ni crees schedulers separados. XTTS es opcional y no debe forzar
coexistencia GPU con Qwen.

## 23. Recuperacion semantica focalizada de paginas dificiles (2026-08-09)

### Contrato y limites

La pagina completa mantiene el flujo primario y su verificador. Solo si la
cobertura estricta continua incompleta, `PdfSemanticRecoveryPlanner` puede
justificar una unica ronda focalizada de hasta tres ROIs. Los motivos tipados son
tabla densa incompleta, hueco de texto fiable, hueco/orden multicolumna y region
visual incompleta. No existe tiling arbitrario ni recovery basado en una capa
PDFBox clasificada como no fiable. Si no hay una ROI defendible, el intento se
rechaza explicitamente.

Cada ROI se rasteriza a 300 DPI con `PdfRenderEngine.renderCrop`, reutiliza el
contrato V1 `PAGE/BEGIN/SOURCE/SPEECH/END/DONE` y remapea sus bbox 0..1000 a la
pagina completa en Java. Las tablas exigen todas las celdas visibles, una fila
por linea y separacion reproducible con ` ; `. Los prompts no tabulares usan un
prefijo/sufijo concreto sin contenido ficticio: el smoke encontro que un ejemplo
semantico podia ser copiado literalmente y detener la lectura.

`PdfSemanticRecoveryMerger` deduplica conservadoramente por texto/geometria,
reemplaza una TABLE parcial por su TABLE completa, conserva la identidad de la
respuesta base y registra `replacedResponseId`, `recoveryResponseId`,
`semanticPass` y `recoveryReason`. El orden final es determinista y consciente de
dos columnas/separadores; no depende del orden de finalizacion de inferencias.
Numero de pagina, confianza base y `contentId` canonico se conservan. El
`PdfSemanticCoverageValidator` no se relajo.

La publicacion sigue siendo atomica: solo la pagina final que vuelve a superar
la cobertura sustituye la `PreparedPdfPage`. Protocolo invalido, falta de DONE,
recovery insuficiente, cancelacion o error conservan la pagina canonica anterior
y el bruto/diagnostico disponible. No hay retry identico automatico.

### Recursos y telemetria

Qwen permanece serial 1/1/1 y usa la unica admision global. Se encontro y
corrigio un deadlock real: el batch semantico ya poseia CPU_HEAVY/QWEN y el crop
intentaba readquirir CPU_HEAVY con capacidad 1. El crop ahora se ejecuta dentro
de la concesion existente; no se creo scheduler adicional ni bypass. La
telemetria por pagina registra cantidad de ROIs, motivos, duracion, prompt tokens
y output tokens, y el progreso UI expone `TARGETED_RECOVERY`/`RECOVERY_CROP`.

### Prueba fisica

El corpus fisico uso Qwen3-VL 4B Q8, contexto 8192 y modelo residente entre
peticiones. Evidencia y bruto quedaron en
`target/semantic-recovery-physical/`.

- Tabla densa de `informe.pdf`, ROI materializada en
  `target/qwen-concurrency2-experiment/20260809-154104/table-A-roi.png`:
  22/22 anclas, al menos siete filas estables, todas las celdas significativas,
  378 tokens de salida y 3.453 tok/s. El control conjunto tardo 228.719 ms.
- Columna tecnica de `EdsacDoc.pdf`, pagina 10, crop
  `tmp/pdfs/recovery-corpus/edsacdoc-p10-right.png`: 15/15 anclas, llaves/notas y
  texto de inicio a fin preservados, protocolo completo; 84.838 ms.

El primer intento multicolumna fue rechazado por faltar campos del protocolo. El
segundo fue rechazado porque copio solo el contenido del ejemplo. Cada repeticion
uso una estrategia materialmente distinta y documentada; no hubo retry identico.
El tercer intento fue aceptado con prefijo/sufijo sin ejemplo semantico y
evidencia explicita.

### Validacion y deuda deliberada

La regresion dirigida PDF/Word paso, incluyendo DOCX import/diagnosticos/bloques
visuales/narratabilidad, `ReadableDocument`, repositorios, guion, publicacion PDF
incremental, narration bindings/highlight, playback, scheduler y lifecycle Qwen.
La suite completa termino con 1.266 pruebas, 0 fallos, 0 errores y 18 omitidas.
La revision `--check` individual no encontro errores de whitespace; Git solo
aviso de su conversion configurada LF a CRLF en el working copy.

Queda deliberadamente pendiente ampliar el corpus fisico a formulas/figuras y
mas disposiciones multicolumna, y ejecutar el gate manual JavaFX de navegacion,
pause/resume/stop y reapertura/cache: la app habia sido cerrada. Esta fase no
abre concurrencia Qwen, no cambia PDF canonico a `DocumentBlock`, no cambia
exportacion ni implementa generacion ilustrativa.

# Chat handoff — copiar a una nueva conversacion

La recuperacion semantica focalizada de paginas dificiles esta implementada y
validada. Tras primary+verifier, una sola ronda de hasta tres ROIs justificadas
puede recuperar TABLE densa, texto fiable omitido, columna o region visual. Usa
crop PDF existente a 300 DPI, protocolo por bloques V1, bbox remapeada en Java,
merge/dedupe/readingOrder deterministas, provenance explicita y publicacion
atomica. CoverageValidator permanece estricto y una respuesta parcial nunca
reemplaza la pagina canonica.

El smoke Q8/8K serial paso: tabla 22/22 anclas y filas/celdas completas a 3.453
tok/s; columna de EDSAC pagina 10, 15/15 anclas y 84.838 ms. Evidencia en
`target/semantic-recovery-physical/SEMANTIC_RECOVERY_PHYSICAL_REPORT.md`. Suite
completa: 1.266 pruebas, 0 fallos/errores, 18 omitidas; Word focal verde. Se
corrigio la readmision CPU_HEAVY anidada que podia bloquear capacidad 1. Qwen
sigue 1/1/1 y no existe scheduler separado.

Lo pendiente es un gate visual/manual JavaFX y ampliar corpus fisico para
formula/figura y mas layouts. No avances a concurrency Qwen sin una fase y
mediciones separadas. Conserva PreparedPdfPage/PdfRegion como canon PDF,
ReadableDocument/DocumentBlock como canon Word y NarrationSegment como
convergencia de audio.

## 24. Corpus final de robustez y aceptacion PDF (2026-08-10)

### Alcance y criterio de adjudicacion

Se ejecuto un corpus fisico legitimo de doce paginas, sin fabricar PDFs para
obtener resultados favorables. El runtime se mantuvo en Qwen3-VL 4B Q8, contexto
8192 y admision serial 1/1/1. El criterio fue `SUPPORTED AND CORRECT` frente a
`DETECTED AS UNSAFE`; un rechazo estricto cuenta como resultado correcto cuando
la alternativa seria publicar contenido incompleto. No se habilito concurrencia,
no se cambio el dominio canonico y no se relajo `CoverageValidator`.

Los artefactos por ejecucion estan en `target/pdf-final-acceptance/`. Cada caso
conserva respuesta bruta, metricas, eventos, evidencia nativa, resultado y, cuando
hubo publicacion, repositorio reabrible. La evidencia fisica adicional de TTS y
documento multipagina permanece en `target/fast-listen-physical/`.

### Matriz fisica final

| Caso | Categoria | Native | Flujo | Resultado adjudicado | Regiones / narracion | Tiempo; decode | Observacion |
|---|---|---|---|---|---|---|---|
| `demo-p1` | prosa, matematica, figura | fiable | primary + verifier | ACCEPTED | 9; 6 segmentos/bindings | 620.090 ms; 1.907-1.957 tok/s | bbox, orden, anclas y explicacion visual validos |
| `demo-p2` | tabla simple, matematica | fiable | primary only | ACCEPTED | 14; 13/13 | 500.533 ms; 2.311 tok/s | tabla y formulas preservadas; exigir tipo MATH separado era un oraculo incorrecto |
| `demo-p3` | grafica, matematica, prosa | fiable | primary + verifier | REJECTED | no publicado | 437.229 ms; 2.184-2.331 tok/s | verifier uso narratabilidad numerica; rechazo de protocolo seguro |
| `informe-p2` | tabla densa | fiable | primary | REJECTED | no publicado | 601.715 ms; 2.233 tok/s | region 7 sin control SOURCE; no se rescato salida ambigua |
| `edsac-p10` | codigo y multicolumna | sospechoso | primary + verifier + 1 ROI | REJECTED | no publicado | 369.229 ms; 2.375-3.171 tok/s | faltaban celdas/anotaciones; omision ya no se publica |
| `edsactg-p10` | prosa tecnica | fiable | primary only | ACCEPTED | 7; 7/7 | 484.862 ms; 2.304 tok/s | contenido, bbox, orden y reopen correctos |
| `electronics-p10` | prosa y formulas | fiable | primary only | ACCEPTED | 9; 6/6 | 347.162 ms; 2.366 tok/s | formulas y texto preservados sin verifier innecesario |
| `kress-p20` | matematica y prosa | inutilizable | primary | REJECTED | no publicado | 357.716 ms; 3.170 tok/s | bloque sin SOURCE; protocolo estricto |
| `kress-p335` | indice multicolumna | inutilizable | primary + verifier | REJECTED | no publicado | 62.326 ms; 3.268-4.356 tok/s | indice sospechosamente reducido; blocker de pagina estructurada dispersa |
| `burden-p20` | grafica, matematica, prosa | fiable | primary + verifier + 3 ROI | REJECTED | no publicado | 658.837 ms; 2.307-3.654 tok/s | recovery siguio dejando texto significativo; rechazo por cobertura |
| `kress-p1` | portada raster, sin native fiable | inutilizable | primary | REJECTED | no publicado | 111.579 ms; 2.666 tok/s | region sin SOURCE; no falsa cobertura |
| `wwg-p10` | escaneada casi vacia | inutilizable | primary + verifier + 1 ROI | REJECTED | no publicado | 203.898 ms; 2.043-3.741 tok/s | `noSubstantiveVisibleContent`; no publica una IMAGE que solo dice pagina vacia |

Resumen del corpus: 12 muestras; 4 ACCEPTED y 8 REJECTED de forma explicita;
3 accepted primary-only; 1 accepted tras verifier; 0 accepted tras recovery en
esta tanda; verifier usado en 6 casos; recovery ejecutado en 3 casos publicados
en la matriz como 1, 3 y 1 ROIs; fallback implicito 0. Los controles fisicos de
la fase anterior siguen verdes para recovery exitoso: tabla densa 22/22 a 3.453
tok/s y columna EDSAC 15/15 en 84.838 ms. El rango de decode observado en este
corpus fue 1.907-4.356 tok/s. El T0-T4 fisico de FAST_LISTEN sigue siendo 302.011
ms, con primer WAV Piper 5.425 ms despues de disponer de segmentos.

### Defectos reales corregidos

- El parser normaliza aliases fisicos sin ampliar el dominio: `HEADLINE` y
  `SECTION_TITLE` a HEADING; `SUBSECTION_TITLE`/`SUBTITLE` a SUBHEADING; `TEXT`
  a PARAGRAPH; `DEFINITION` a SIDEBAR; `FIGURE` se decide de forma conservadora
  entre IMAGE y CAPTION. Cada alias queda en provenance.
- El prompt diferencia CODE de TABLE y exige comentarios, llaves y anotaciones
  laterales; SOURCE y SPEECH deben ser controles exactos.
- El verificador ya exige exactamente ocho campos totales en BEGIN, una sola
  caja y narratabilidad N/X/U en el campo 7. No se normalizan numeros ambiguos.
- `PdfBoxNativeTextEvidenceExtractor` reconstruye lineas desde fragmentos por
  palabra sin unir columnas/celdas separadas. Asi `Hallazgos en una pagina` deja
  de quedar invisible para cobertura.
- `suspiciousSparseStructuredPage` impide aceptar indices/catalogos casi vacios;
  `noSubstantiveVisibleContent` impide publicar una pagina vacia como IMAGE.
- El filtro de margenes evita que headers/footers repetitivos provoquen falsos
  rechazos, sin disminuir el umbral de tablas ni perder bloques sustantivos.

Durante el reconocimiento aparecieron cuatro falsos aceptados y fueron cerrados:
EDSAC sin anotaciones laterales, indice Kress reducido a `Index`, tabla de informe
sin su encabezado y pagina WWG vacia descrita como imagen. Bajo la adjudicacion
final ninguno se publica. Los falsos rechazos reproducibles por aliases o por el
oraculo MATH separado quedaron corregidos. `silent omissions` conocidas entre las
cuatro paginas finalmente ACCEPTED: 0.

### Persistencia, audio, highlight y fallos

La regresion transversal cubre publicacion atomica, conservacion de pagina valida
ante intento/protocolo/recovery fallido, cache y reopen, fingerprints, invalidez
selectiva por unidad, `PreparedPdfPage -> NarrationSegment ->
PdfNarrationBinding`, highlight a la region/bbox completa, dedupe y reutilizacion
de WAV, playback desde la seleccion y `PROCESS_COMPLETE` sin autoplay. Tambien
cubre cancel Qwen/TTS, timeout, truncacion sin DONE, protocolo invalido, recovery
insuficiente, TTS fallido y muerte de proceso donde existe adaptador testeable.

El smoke fisico multipagina con `Demostracion_limite_notable.pdf` confirma modelo
residente, P1/P2 terminadas en DONE, Piper real, WAV valido y solapamiento Qwen P2
+ TTS P1. No se sintetizo inutilmente todo el corpus. Word permanece separado y
verde: importacion DOCX, diagnosticos, bloques visuales, narratabilidad, assets
semanticos y acciones de audio pasaron sin regresion.

### Gate JavaFX y checklist manual exacta

No se invento un resultado visual. Se intento usar el mecanismo legitimo de
control de Windows; pudo identificar y cerrar normalmente la ventana de la app,
pero el paquete instalado del conector no pudo activar la sesion interactiva
(`sky.documentation is not a function` / contexto de ejecucion no disponible).
El criterio de esta fase permite cerrar con checklist manual explicita pendiente:

1. Abrir `D:\Proyectos\Demostracion_limite_notable.pdf` dentro de un proyecto.
2. Navegar a P2 y seleccionar una region de esa pagina.
3. Pulsar FAST_LISTEN y confirmar que inicia en P2/region, no en P1.
4. Confirmar que el resaltado coincide con la region/bbox narrada.
5. Pausar, esperar, reanudar y comprobar continuidad sin regeneracion.
6. Detener y confirmar que no se eliminan paginas, WAV ni cache validos.
7. Durante preparacion/prefetch navegar P1 -> P3 -> P2 y comprobar que no cambia
   el orden canonico ni se destruyen resultados ya aceptados.
8. Cerrar y reabrir el proyecto; repetir FAST_LISTEN desde P2 y confirmar fast
   path sin nueva llamada Qwen/TTS cuando fingerprints siguen vigentes.

### Validacion y readiness

La suite final del working tree termino con 1.279 pruebas, 0 fallos, 0 errores y
19 omitidas optativas. La regresion focal Word/PDF/audio/persistencia paso.
`git diff --check` no encontro errores; solo informo la conversion LF->CRLF
configurada para `studio-desktop/pom.xml`.

La fase de aceptacion queda VERDE: existe corpus representativo, no hay omisiones
silenciosas conocidas en paginas aceptadas, los rechazos son atomicos, la tabla
densa y multicolumna conservan controles fisicos previos, matematica/figura/
escaneado/native defectuoso estan cubiertos, las paginas faciles no pagan recovery
innecesario, cache/narracion/highlight/FAST_LISTEN core/Word/suite estan verdes y
la unica observacion manual tiene checklist exacta. El lector esta listo para la
tanda final de cierre y congelamiento. No se debe abrir mas trabajo funcional ni
reabrir concurrencia Qwen.

# Chat handoff — copiar a una nueva conversacion

DocuPodcast Studio completo la fase final de robustez previa al freeze. Se
ejecutaron 12 paginas fisicas legitimas con Qwen3-VL 4B Q8, contexto 8K y runtime
serial 1/1/1: 4 fueron ACCEPTED con bbox/orden/contenido/reopen/narracion validos
y 8 fueron REJECTED atomicamente por protocolo o cobertura. Hubo 3 accepted
primary-only, 1 accepted tras verifier y 0 omisiones silenciosas conocidas entre
las aceptadas. No interpretes los rechazos como fallos de seguridad: son
`DETECTED AS UNSAFE` y no publicaron paginas parciales.

Se corrigieron defectos minimos observados: aliases semanticos a tipos canonicos,
reconstruccion de lineas PDFBox, guardas para indices dispersos y paginas vacias,
distincion CODE/TABLE y contrato de verifier con exactamente una bbox, ocho campos
y N/X/U. El parser y CoverageValidator siguen estrictos. Los controles previos de
recovery fisico permanecen verdes: tabla densa 22/22 y columna EDSAC 15/15. Piper
fisico, WAV, overlap Qwen+TTS y T0-T4=302.011 ms estan en
`target/fast-listen-physical/`; el corpus esta en
`target/pdf-final-acceptance/`.

Cache/reopen, invalidacion selectiva, publicacion atomica, narracion, bindings,
highlight, cancelacion/fallos, FAST_LISTEN core y regresion Word estan verdes.
Suite final: 1.279 pruebas, 0 fallos, 0 errores, 19 optativas omitidas. El unico
gate no observado con clicks es JavaFX porque el conector local fallo; la seccion
24 contiene la checklist manual exacta. La fase se considera verde bajo su propio
criterio, que permite checklist explicita. Solo falta la tanda de CIERRE TECNICO Y
CONGELAMIENTO DEL LECTOR PDF: limpiar experimentos/codigo muerto, retirar rutas
obsoletas, revisar versionado/logs/UI, consolidar smokes y documentar limites. No
anadas funcionalidad, no cambies el dominio y no reabras concurrency Qwen.

# PDF semantic reader — FROZEN

Fecha de congelamiento: 2026-08-10. El cierre eliminó el parser JSON y los
harnesses experimentales de concurrencia/replay, dejando Block V1 como transporte
productivo único. La firma compatible es
`pdf-semantic-reader-block-v1-frozen-2026-08-10`; manifest y páginas permanecen
en V3 y las páginas antiguas se regeneran de forma segura.

Los umbrales congelados son 0.84 global, 0.58 para bloque largo y 0.80 para
bloque corto. Este último sustituye al 0.68 histórico por evidencia de omisiones.
La confianza Qwen no es autoridad. Evidencia insuficiente persiste como
`INSUFFICIENT_EVIDENCE`; errores técnicos y cancelación se mantienen separados.

Las autoridades de mantenimiento son
`docs/architecture/pdf-semantic-reader.md`,
`docs/operations/pdf-semantic-reader-smokes.md` y
`docs/operations/pdf-semantic-reader-freeze-2026-08-10.md`. No se debe iniciar
otra fase PDF salvo defecto reproducible o decisión arquitectónica expresa con
nuevo corpus. El siguiente trabajo del producto debe abrir una línea distinta.

# Chat handoff — copiar a una nueva conversación

El lector PDF semántico de DocuPodcast Studio está terminado y congelado. Block
V1 es la única ruta, la publicación es atómica, la recuperación está acotada y
la concurrencia Qwen permanece cerrada. Consulta los tres documentos canónicos
antes de tocar el componente. No planifiques una nueva fase PDF por continuidad;
solo reábrelo ante un bug reproducible o una decisión explícita con evidencia.

Regresión de cierre: 1.272 pruebas vigentes, 0 fallos, 0 errores y 13 smokes
opt-in omitidos. Word, audio, video, recursos, persistencia y lifecycle quedaron
verdes. `git diff --check` no halló errores de whitespace.

## Corrección post-freeze de outcomes — 2026-08-10

El lector continúa congelado. La evidencia real mostró P1 `COMPLETED` con 9
regiones, P2 dos veces `TRUNCATED` y después `FAILED` por terminación del runtime,
y P3 `INSUFFICIENT_EVIDENCE`. Solo P1 tiene página canónica, por lo que el hover
es correcto únicamente allí.

Se eliminó la `IllegalStateException("Páginas fallidas: ...")` artificial. El
scope conserva estado/categoría/causa por página, distingue total/partial/failure,
continúa tras incidencias locales, contabiliza terminal versus aceptada y usa copy
específico. El aviso de copia de fuente quedó no modal y diferido, corrigiendo el
`showAndWait` durante layout. Suite posterior: 1.280 pruebas, 0 fallos, 0 errores,
13 opt-in omitidos. Informe:
`docs/operations/pdf-post-freeze-page-outcomes-2026-08-10.md`.
