# DOC-UX-HF9F — importación con progreso y playback más estable

## Objetivo
Cerrar los hallazgos posteriores a DOC-UX-HF9E: diagnóstico con tres source tests pendientes, repeticiones ocasionales de frases durante reproducción por unidades y bloqueo perceptible al abrir documentos fuente grandes.

## Cambios
- Se encapsula el diálogo de motor no disponible en `AudioEngineUnavailableDialog`; el shell ya no crea `Alert` directamente.
- La apertura de fuente documental desde el FileChooser usa `Task<ReadableDocument>` y `DocumentImportProgressDialog`, de modo que la lectura/clasificación ocurre fuera del hilo JavaFX.
- `DocuPodcastShellViewModel` separa `importAndClassifySourceDocument(...)` de `attachImportedDocument(...)` para permitir importar en background y aplicar resultados en el hilo UI.
- `cueForCursor(...)` ya no cae a la primera cue del segmento cuando existen varias unidades/oraciones; prefiere la cue por posición o la unidad más cercana.
- Se conservan las cadenas de guardarraíl de pausa/cancelación segura, pero la pausa del playback sigue desacoplada de la generación.

## Nota de producto
La importación ya no debería congelar la ventana principal al leer documentos grandes, pero el render completo de miles de bloques sigue siendo un tema separado. Queda planificada una tanda posterior de virtualización/paginación del lector para documentos extensos.

## Validación local en entorno ChatGPT
- Source tests focales de diálogos, rail derecho y procesos de playback: OK.
- ZIP íntegro.
- Maven completo no ejecutado por falta de `mvn`.
