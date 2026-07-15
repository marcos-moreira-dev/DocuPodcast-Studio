# PLAYBACK-SELECT1 / MODEL-PORT1 — selección como primer chunk y models portable

Fecha: 2026-06-06

## Contexto

El usuario confirmó dos comportamientos pendientes:

1. Si una carpeta `models/` ya descargada se copia a una nueva tanda, la aplicación debe reconocerla sin forzar una descarga completa nuevamente.
2. Al usar **Reproducir desde selección**, DocuPodcast no debe empezar a preparar desde el primer chunk del documento; debe usar la selección como primer chunk solicitado y continuar desde ahí.

## Cambios

- La inspección de Voz IA avanzada, el descargador del modelo y el comando local de síntesis ahora prefieren una carpeta portable `models/tts/xtts` dentro de la raíz actual de la app cuando contiene el contrato completo del modelo.
- Esto evita que una configuración persistente con rutas absolutas de otra tanda impida reutilizar un `models/` trasplantado.
- `runDocumentPrimaryAction()` ya no cae a `listenToDocument()` completo cuando hay selección sin WAV; delega a la reproducción desde selección.
- `startPlaybackFromSegment(...)` genera audio desde el segmento seleccionado si aún no existe manifest/cue reproducible.
- Se agrega `pendingPlaybackStartSegmentId` para que, cuando el chunk seleccionado se complete durante la generación, la reproducción arranque desde ese chunk y no desde el primer chunk disponible.
- El índice lateral usa un icono tipo árbol (`tree-index.png`) en lugar de reutilizar el icono del rail visual.

## Fuera de alcance

- No se cambia el runtime Python ni se revalida Voz IA avanzada completa.
- No se modifica exportación de audio/video.
- No se hace refactor transversal grande.

## Validación focal

- `domain + application + infrastructure`: compilación focal OK sin JavaFX.
- Tests fuente nuevos/reforzados:
  - `PlaybackSelect1SourceTest`
  - `PortableModelsTransplantSourceTest`
  - refuerzo de `DocumentIndexHf13SourceTest`
