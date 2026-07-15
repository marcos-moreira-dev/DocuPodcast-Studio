# 30 — Tanda 7 plan: Storyboard vivo

## Objetivo

Permitir imagen por segmento y reproducción sincronizada básica.

## Dominio

```text
MediaAsset
StoryboardDocument
StoryboardScene
StoryboardBinding
PlaybackManifest
PlaybackCue
```

## UI

```text
StoryboardWorkspaceView
StoryboardCanvasAdapter
StoryboardRenderKit
StoryboardScenesPanel
MediaLibraryPanel
ImagePropertiesPanel
```

## MVP

- Importar imagen.
- Asociarla a segmento.
- Mostrar tarjeta de escena.
- Resaltar durante playback.

## No hacer aún

- Animaciones.
- Video complejo.
- Varias imágenes por rango interno.
