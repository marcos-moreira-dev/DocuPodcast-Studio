# DOC-UX-HF9G — apertura documental simple y tamaño estimado de audio

## Objetivo

Cerrar dos ajustes de UX detectados tras HF9F:

1. Abrir una fuente documental no debe comunicarse como preparación de fragmentos/audio; solo lee la fuente y la muestra en Documento.
2. El panel flotante de preparación de audio debe informar el tamaño estimado de todos los chunks para que el usuario decida si espera o cancela.

## Cambios

- `DocumentImportProgressDialog` ahora habla de abrir/leer la fuente documental para mostrarla en Documento.
- `DocuPodcastShellView.runSourceDocumentImport(...)` deja de mostrar `Leyendo documento y preparando fragmentos...`.
- El diálogo de importación se cierra antes de adjuntar/renderizar el documento, usando un pequeño pulso (`PauseTransition`) para evitar que parezca atascado encima de la vista ya cargada.
- `LongProcessOverlayView` agrega `tamaño estimado` al panel de preparación de audio.
- El tamaño se calcula a partir de los WAV ya completados en el directorio del job y se proyecta al total de segmentos.
- Si todavía no hay chunks generados, muestra `tamaño estimado: calculando`.
- `DocuPodcastShellView` conserva el texto `Motor de voz no disponible` para el guardarraíl fuente de confirmación humana.

## Alcance no incluido

- No implementa virtualización/paginación real del lector para documentos enormes. Eso queda para `DOC-PERF-HF10`.
- No cambia motores Coqui/Voz IA avanzada. La verificación real queda para `MOTOR-SMOKE4R`.
- No cambia la generación de chunks, solo la comunicación de importación y el cálculo de tamaño aproximado del audio.

## Validación focal

- `DocUxHf9EPlaybackGenerationSeparationSourceTest`
- `DocUxHf9FImportProgressPlaybackSourceTest`
- `DocUxHf9GImportOverlaySizeSourceTest`
