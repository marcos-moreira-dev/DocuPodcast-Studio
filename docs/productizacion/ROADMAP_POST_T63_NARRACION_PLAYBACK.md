# Roadmap post T63 — Cerebro primero, cara después

## Base actual

T63 deja extraídos los primeros coordinadores de narración/playback desde Documento:

- `DocumentIntakeCoordinator`
- `SourceDocumentRefreshCoordinator`
- `WorkspaceNavigationCoordinator`
- `DocumentNarrationCoordinator`
- `PlaybackWorkflowCoordinator`

## Tandas pendientes recomendadas

| Tanda | Foco | Criterio de salida |
|---|---|---|
| T64 | `AudioWorkflowCoordinator` | Generación, reanudación, cancelación y manifest dejan de depender directamente del ViewModel. |
| T65 | `NarrativeLayerCoordinator` | Voz/audio/emoción/imagen/ambiente/notas se asignan desde Documento con targets reales o estados pendientes honestos. |
| T66 | Round-trip funcional real | Abrir, escuchar, asignar capas, asociar imágenes, guardar, cerrar y reabrir sin pérdida. |
| T67 | Configuración operativa | Persistir y probar motor TTS, STT, FFmpeg, buffer, rutas y diagnóstico. |
| T68 | Rediseño UI aplicado | Limpiar Documento/Inicio con criterios Word-like sin cabina técnica. |
| T69 | Storyboard/video operativo | Imagen asociada dura lo que dura el texto hablado; paquete renderizable o MP4 real según FFmpeg. |
| T70 | Smoke integral | Matriz manual con documentos simples, largos, técnicos, diálogos, errores y recuperación. |
| T71 | Packaging/RC | App-image/MSI, hashes, manifiestos, licencias, guía y limitaciones conocidas. |

## Regla de orden

No avanzar a rediseño visual fuerte antes de tener el cerebro de Documento/audio/playback/capas suficientemente separado y testeable.
