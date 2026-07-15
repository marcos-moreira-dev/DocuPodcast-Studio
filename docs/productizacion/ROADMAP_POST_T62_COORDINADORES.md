# Roadmap post T62 — cerebro primero, cara después

## Base tras T62

T62 introduce los primeros coordinadores prioritarios:

- `DocumentIntakeCoordinator`
- `SourceDocumentRefreshCoordinator`
- `WorkspaceNavigationCoordinator`

El botón **Refrescar contenido** queda preparado como acción real: relee la fuente solo lectura, compara snapshots y actualiza el Documento narrable del proyecto sin sobrescribir el archivo externo.

## Próximas tandas sugeridas

### T63 — Refactor de reproducción/audio desde Documento

Extraer de `DocuPodcastShellViewModel` el flujo Documento → escuchar → audio → manifest → playback.

Candidatos:

- `DocumentNarrationCoordinator`
- `PlaybackWorkflowCoordinator`
- `AudioWorkflowCoordinator`

### T64 — Round-trip funcional real

Probar guardar/cerrar/reabrir con Documento narrable, capas, audio, storyboard, imágenes y estado de refresco.

### T65 — Configuración operativa

Hacer que buffer, TTS, STT, FFmpeg, rutas de modelos y diagnóstico sean configurables y persistentes.

### T66 — Rediseño UI aplicado

Aplicar el catálogo de componentes transversales y los principios Word-like solo cuando el cerebro ya tenga coordinadores suficientes.
