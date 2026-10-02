# Hoja de ruta de preparación PDF

## Tandas 1–7

Completadas: modelo V2, persistencia por página, preparación progresiva,
alcances, audio trazable, extracción native-first, layout transversal y
tratamientos derivados optativos. La narración determinista de tablas pequeñas
permanece disponible; no se anuncian modelos especializados inexistentes.

## Tanda 8A — Sesión y persistencia

Completada:

- `ProjectDocumentSource` separa documentos por bloques y PDF preparado;
- importación PDF crea staging V2 sin `ReadableDocument`;
- apertura, guardado y Guardar como conservan manifest, páginas, IDs y
  overrides;
- integridad valida manifest y SHA-256 de la fuente;
- un cierre sin guardar elimina solo el staging administrado;
- PDF nunca escribe `document/document.json`.

## Tanda 8B — Consumidores y selección

Completada:

- selección unificada por bloque o región PDF;
- snapshot compartido con procedencia, página, geometría, revisión y estado;
- visor, resaltado, índice, búsqueda, contexto, overrides, narración y
  localización inversa consultan `PreparedPdfDocumentRepository`;
- búsqueda ordinaria no agenda OCR;
- `UNCERTAIN` permanece fuera del TTS;
- el índice de regiones/headings es volátil y se invalida al publicar.

## Tanda 8C — Retirada de compatibilidad

Completada:

- narración PDF separada en `BuildPreparedPdfNarrationUseCase`;
- retirados los proyectores y resolutores PDF basados en `ReadableDocument`;
- retirados importer y selectores que fabricaban bloques PDF;
- el mapper OCR/nativo produce exclusivamente `PreparedPdfPage`;
- una prueba arquitectónica prohíbe símbolos y metadatos heredados;
- solo el scheduler puede invocar la preparación unitaria.

## Tanda 8D — Corpus y cierre

Completada:

- corpus PDFBox reproducible para digital, escaneado, híbrido, rotado,
  multicolumna, tabla, fórmula y OCR defectuoso;
- recorrido integral de importación a diagnóstico;
- overlays de regiones y reporte JSON de métricas en
  `target/pdf-v2-corpus`;
- navegación larga 1→150→20 congela promoción, deduplicación y un solo
  trabajador;
- smoke Tesseract real y suite completa forman la puerta final de entrega.

## Tandas 9–17 — estado de implementación

### Tanda 9 — Corpus real

Implementada la identidad reproducible de Kress (342 páginas) y Burden (895),
las ocho páginas anotadas, overlays y reporte opt-in en
`target/pdf-real-corpus`. Los originales solo se leen. El recorrido integral de
las 1.237 páginas dispone de una puerta opt-in real que usa el pipeline
native-first, Tesseract administrado, persistencia por página y un único
trabajador. Permanece fuera de CI por duración.

### Tanda 10 — Extracción regional y escalabilidad

Implementados calidad por región, idioma local con override, rol de página,
PSM adaptativo, firma de preparación y actualización incremental del resumen de
layout. Se corrigió la truncación real de `bbox-layout`: Burden puede evitar OCR
cuando su capa nativa es fiable; Kress continúa por Tesseract.

### Tanda 11 — Revisión visual

Completada la superficie de revisión de la región seleccionada: permite
solicitar descripción visual, reconocimiento/lectura matemática o corrección
contextual, muestra el resultado persistido y permite aprobarlo o rechazarlo.
Regiones, evidencias candidatas, narratabilidad, idioma, revisiones y overrides
continúan siendo independientes. Ningún resultado `DRAFT` llega al TTS.

### Tanda 12 — PP-StructureV3

Adaptador real, importación explícita, readiness, smoke y script sin descargas
internas implementados. La descarga automática no se publica porque Paddle no
ofrece un único paquete portable fijado que satisfaga checksum y atomicidad; el
usuario importa un paquete validado desde Motores y dependencias.

### Tanda 13 — Matemáticas narrables

Reconocimiento avanzado conectado a PP-Structure, conversión conservadora
LaTeX→MathML e integración local MathCAT4J terminados. MathCAT y sus reglas
españolas/inglesas se distribuyen como dependencia integrada, sin descarga ni
servicio externo. Las construcciones LaTeX no soportadas se rechazan para
revisión en vez de aproximarse; toda lectura queda `DRAFT` hasta aprobación.

### Tanda 14 — Qwen3-VL embebido

Implementados runtime Ollama standalone administrado, puerto privado,
`OLLAMA_MODELS` local, PID/logs, espera de readiness, cierre del árbol, Q8/Q4,
preflight, preparación explícita, persistencia de la elección manual Q4/Q8 y
smoke con imagen real. Los motores implementan `ContentAnalysisEngine`; no
conocen PDF y sirven transversalmente a toda la aplicación. Sus capacidades
se llaman `visual-content-description` y `content-context-correction`, no
`document-*`.

### Tanda 15 — Corrección contextual

Implementado sobre el mismo Qwen y runtime. Solicita JSON estructurado, limita
el contexto y rechaza propuestas que pierdan cifras, unidades, nombres u
operadores. El resultado queda `DRAFT`.

### Tanda 16 — Rectángulo sincronizado

Implementados foco persistido en el manifiesto de audio, huella granular y
rectángulos accesibles en el visor. Pausa conserva el foco; seek y cambio de cue
lo mueven; stop y cierre lo limpian.

### Tanda 17 — Cierre

Pendientes antes de declararla terminada:

- smoke real Qwen Q4/Q8 y PP-Structure con recursos preparados;
- pruebas de OOM, puerto ocupado, cierre abrupto y falta de espacio;

Completadas en el cierre actual:

- suite Maven completa, arquitectura y UTF-8;
- smoke con Tesseract administrado real;
- corpus real de muestra Kress/Burden con overlays;
- corpus completo Kress/Burden de 1.237 páginas;
- contratos y pruebas JavaFX/accesibilidad automatizadas.

## Tandas 18–22 — cierre operativo de IA local

### Tanda 18 — Cómputo y procesos administrados

Implementada:

- `ComputePreference` neutral permite `AUTO`, CPU explícita, GPU preferida o
  dispositivo específico, con RAM de apoyo como decisión independiente;
- la configuración global propaga GPU/RAM a todos los motores de análisis sin
  imponer un límite artificial de 4 GB;
- PP-Structure recibe el dispositivo solicitado y nunca cambia a CPU de forma
  silenciosa después de un OOM;
- Ollama usa loopback y puerto privado, reintenta carreras de puerto y no adopta
  servidores externos;
- en Windows el proceso queda dentro de un Job Object con
  `KILL_ON_JOB_CLOSE`, además del respaldo por `ProcessHandle`;
- PID, puerto, dispositivo solicitado y efectivo, RAM, VRAM y tiempos quedan en
  diagnósticos copiables.

### Tanda 19 — Recursos reproducibles

Implementada:

- runtime Ollama 0.32.5 fijado por tamaño y SHA-256, con preflight y margen de
  espacio del 15 %;
- validación de manifest y de cada blob de Qwen Q4/Q8, no solo del nombre del
  tag;
- PP-Structure ya no acepta `.venv`, `pyvenv.cfg`, rutas absolutas ni paquetes
  trasladados;
- el paquete portable PP declara Python, Paddle, PaddleOCR, bridge, modelos,
  tamaños, SHA-256, licencias y perfil CPU/GPU;
- mientras no exista un paquete distribuible validado, la interfaz publica
  únicamente «Importar análisis avanzado», nunca un instalador ficticio.

### Tanda 20 — Certificación física

Implementada la puerta:

- `EngineCertificationRecord` se guarda como estado operativo fuera de los
  proyectos;
- una instalación válida pero sin smoke físico es `DEGRADED`;
- solo la combinación runtime, modelo y hardware certificada es `READY`;
- Qwen exige una imagen real y consulta `/api/ps`;
- PP registra geometría, perfil y dispositivo efectivo;
- MathCAT certifica una lectura MathML real.

La evidencia física Q4/Q8 y PP se registra por separado. Un certificado
corrupto, obsoleto o de otro hardware nunca declara listo el motor.

### Tanda 21 — Experiencia documental y filtro de galimatías

Implementada:

- Estudio documental ofrece manualmente descripción visual, lectura matemática,
  corrección contextual y revisión de galimatías de la página;
- todos los resultados nacen como `DRAFT` y permiten aprobar, rechazar,
  regenerar y copiar detalles;
- una dependencia ausente abre Motores y dependencias y permite un único
  reintento;
- el filtro semántico usa Qwen mediante la capacidad neutral
  `content-narratability-analysis`;
- Qwen recibe todas las regiones de la página con sus IDs estables, tipo,
  decisión automática y texto;
- responde una decisión por región: `NARRATABLE`, `NON_NARRATABLE` o
  `UNCERTAIN`, razón y confianza;
- la propuesta jamás borra o reescribe texto. Al aprobarla solo aplica overrides
  de narratabilidad; búsqueda, geometría, evidencia OCR/nativa y texto canónico
  permanecen intactos;
- una revisión dudosa sigue excluida del TTS hasta decisión humana.

La revisión se ejecuta una vez por revisión efectiva de la página y se reutiliza
si nada cambió. Por ello su coste es una inferencia textual por página
solicitada, no una inferencia por región ni por reproducción.

Medición física del 28 de julio de 2026 sobre seis regiones representativas:

- Q8: 89.187 ms en frío y 73.491 ms con el modelo caliente;
- Q4: 60.645 ms en frío y 48.974 ms con el modelo caliente;
- ambos perfiles conservaron la prosa, excluyeron metadatos estructurales y
  dejaron el fragmento ambiguo como `UNCERTAIN`;
- Q4 no sustituye silenciosamente a Q8: la elección de modelo sigue siendo
  explícita y el motor se mantiene desacoplado del modelo.

### Tanda 22 — Fallos y cierre

Automatizadas las puertas de puerto ocupado y carrera de puerto, proceso hijo,
timeout, cancelación, salida prematura, OOM, espacio insuficiente, recurso
corrupto, staging y cierre abrupto. La falta de espacio usa un probe controlado;
las pruebas nunca llenan físicamente el disco.

El cierre requiere, además de la suite completa, evidencia física para Q4, Q8 y
PP-Structure. Si un recurso pesado no está preparado, el código y sus pruebas
simuladas pueden estar verdes, pero la característica continúa declarada
`DEGRADED` y no `READY`.

Q4 y Q8 ya tienen certificación visual física vigente en el equipo de prueba.
PP-Structure continúa en `DEGRADED`: la aplicación acepta únicamente la
importación de un paquete portable verificable y no publica una instalación
ficticia ni acepta un entorno virtual trasladado.

## Capacidades deliberadamente no activadas

- ejecución PP-Structure hasta importar un paquete validado;
- ejecución Qwen hasta que el usuario prepare explícitamente runtime y modelo;
- más de un OCR simultáneo;
- servicios remotos.

Todas las dependencias especializadas se administran exclusivamente desde
Motores y dependencias. Ninguna página inicia descargas.

## Verificación de cierre

Ejecución local del 28 de julio de 2026:

- suite Maven final: 1.042 pruebas, 0 fallos, 0 errores y 10 omisiones
  condicionadas;
- smoke con el Tesseract administrado real: correcto;
- corpus real de muestra: Kress 20/100/340 sin capa nativa utilizable;
  Burden 20/50 fiable y 100/500/895 sospechoso, sin perder sus regiones;
- corpus real completo: 1.237 páginas, 684 páginas con OCR, 13.776
  regiones `UNCERTAIN` preservadas, 0 fallos y 1.551.315 ms; los SHA-256 de
  ambos originales fueron revalidados al terminar;
- corpus determinista: 8 páginas, 9 regiones y 2 regiones dudosas;
- texto digital fiable: 0 invocaciones OCR;
- páginas con OCR: 5 de 8;
- baseline informativa del corpus: 2.417 ms, 1.171 ms de CPU observada,
  234.607.792 bytes de heap máximo observado, 18.235 bytes preparados y
  16 archivos canónicos.

Las métricas son una fotografía funcional, no un presupuesto ni una garantía
de rendimiento. El detalle por caso y los overlays se regeneran en
`target/pdf-v2-corpus`.

La puerta completa se ejecuta explícitamente con:

```powershell
mvn -q "-Ddocupodcast.pdf.fullCorpus=true" `
  "-Dtest=PdfRealBooksFullPreparationTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Publica progreso recuperable en
`target/pdf-real-corpus/full-corpus-report.json`; nunca modifica ni copia los
dos originales registrados.

## Reglas permanentes

- No iniciar OCR por una búsqueda ordinaria.
- No narrar `UNCERTAIN`.
- No borrar contenido original al generar un derivado.
- No aceptar callbacks de una sesión cerrada.
- No escribir PDF en `document/document.json`.
- Mantener el PDF fuente intacto y todo procesamiento local.

## Tandas 23–26 — cierre de tablas y cómputo

### Tanda 23

Completada la cola global con admisión atómica, prioridades, FIFO,
envejecimiento acotado, cancelación cooperativa y estado visible. XTTS cede
entre chunks mediante un worker persistente por concesión y Qwen confirma la
liberación de residencia antes de entregar memoria de modelo.

### Tanda 24

Completado el dominio de tablas académicas, estadísticas deterministas,
lectura según naturaleza, edición derivada de celdas, selección explícita de
filas/columnas, foco por celda y resumen Qwen estrictamente fundamentado.
PP-Structure ofrece reconocimiento avanzado manual como derivado `DRAFT`.

### Tanda 25

Completada la política Qwen estricta: Q8 automático, Q4 solo tras selección
manual, certificación obligatoria, verificación de VRAM, sin fallback
silencioso, decisiones conservadoras de narratabilidad y fragmentación de
entrada compacta sin enviar JSON de página.

### Tanda 26

Completadas pruebas automatizadas, smokes físicos Q8/Q4 GPU-first, auditoría
de procesos, harness del tercer PDF académico y suite Maven completa. La
puerta PP física permanece abierta hasta importar o distribuir un paquete
portable verificable; mientras tanto el motor informa `DEGRADED` y Tesseract
continúa como ruta estándar.

### Evidencia añadida

`De_los_subconjuntos_a_la_poda.pdf`:

- 118 páginas;
- SHA-256
  `e1d79ff6607cedbd0381ccf9ce8f472e542e5013588e6ca8ed3055852022fccf`;
- páginas 14, 19, 49, 54, 73, 82, 93, 97 y 102;
- tablas de verdad, matrices, fórmulas, recurrencias, cuadros y comparaciones.

La ejecución opt-in `PdfRealBooksCorpusTest` pasó con 0 fallos y conserva el
original intacto.
