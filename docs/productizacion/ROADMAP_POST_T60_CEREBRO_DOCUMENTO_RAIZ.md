# Roadmap post T60 — cerebro guiado por Documento narrable

## Estado después de T60

T60 fija que el Documento narrable es la raíz conceptual y funcional de V1. El guion queda como proyección interna/avanzada, no como segundo objeto padre para el usuario normal.

## Orden recomendado

| Tanda | Foco | Regla |
|---|---|---|
| T60 | Auditoría ejecutable del cerebro y contrato Documento narrable raíz. | No cambio visual profundo. |
| T61 | Refactor prioritario de coordinadores. | Extraer cerebro sin cambiar experiencia visible. |
| T62 | Round-trip funcional real. | Abrir, escuchar, asociar capas, guardar, reabrir. |
| T63 | Configuración operativa. | Motores, modelos, buffer, FFmpeg, STT y preferencias. |
| T64 | Rediseño UI aplicado. | Inspiración Word/WPS sin edición de fuente. |
| T65 | Smoke integral / RC. | Evidencia manual, límites conocidos y packaging. |

## Efecto sobre T61

T61 debe extraer coordinadores desde la perspectiva del Documento narrable:

- `DocumentIntakeCoordinator`: apertura/perfil de fuente/importación.
- `NarratedDocumentCoordinator`: Documento narrable, diagnóstico y estado principal.
- `DocumentSelectionCoordinator`: bloque/oración/rango.
- `NarrationProjectionCoordinator`: segmentación interna/guion compatible.
- `NarrativeLayerCoordinator`: capas y conflictos.
- `PlaybackWorkflowCoordinator`: manifest, cursor, buffer y transport controls.
- `AudioWorkflowCoordinator`: jobs, generación, recuperación.
- `StoryboardWorkflowCoordinator`: imágenes y bindings.
- `VideoExportCoordinator`: paquete/render/MP4 futuro.
- `SettingsWorkflowCoordinator`: preferencias y pruebas guiadas.

## Efecto sobre T64

La cara puede tomar de Word/WPS:

- página centrada;
- grupos de acciones;
- barra de estado;
- zoom;
- modo lectura;
- navegación por documento.

Pero no debe comunicar edición de fuente. La pantalla principal debe sentirse como lector narrado, no como editor ofimático ni cabina técnica.
