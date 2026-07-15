# Tanda 52 — Flujo Word → escuchar completo

## Propósito

Esta tanda refuerza el contrato principal del producto: un usuario no técnico debe poder abrir un Word/DOCX y entender, desde la pantalla Documento, qué falta para escucharlo sin navegar por Guion, Audio o Jobs.

## Cambios

- Se agrega `DocumentListenFlowState`, una representación de estado en lenguaje de producto.
- `DocuPodcastShellViewModel` expone `documentListenFlowTitleProperty()` y `documentListenFlowDetailProperty()`.
- `DocumentWorkspaceView` muestra una tarjeta compacta con el progreso del flujo Word → escuchar.
- La tarjeta indica si falta abrir documento, preparar guion, guardar proyecto, generar audio, esperar chunks o reproducir.
- El flujo sigue usando la fábrica interna existente: guion, audio, buffer y playback permanecen detrás de la pantalla Documento.

## Contrato de producto

La pantalla principal no debe obligar al usuario a entender `jobs`, `manifest`, `chunks`, `workspace audio` ni rutas técnicas. Debe mostrar mensajes simples:

```text
1. Abre un Word/DOCX
2. Guion narrable listo al escuchar
3. Guarda el proyecto para generar audio
3. Audio listo al escuchar
Preparando audio por chunks
Listo para reproducir
```

## Validación agregada

- `DocumentListenFlowStateTest`
- `ListenDocumentWorkflowSourceTest`

## No cambia

No se modifica exportación, FFmpeg, STT, TTS real, video, persistencia ni selección de capas.
