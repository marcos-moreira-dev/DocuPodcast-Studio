# 15 — Storyboard vivo, imágenes y playback

El storyboard vivo permite asociar imágenes aportadas por el usuario a segmentos del guion. No genera película automáticamente.

## MVP

- Una imagen por segmento.
- Canvas visual con tarjetas de escena.
- Preview de imagen asociada.
- Resaltado de escena activa durante playback.
- Sincronización con texto y audio por segmento.

## Modelo

```text
StoryboardBinding
  id
  segmentId
  imageAssetId
  caption
  displayMode
```

```text
StoryboardScene
  id
  segmentId
  layout
  bindingIds
```

## Canvas

Usar la infraestructura visual inspirada en DMS:

```text
zoom
pan
fit-to-content
capas
selección
arrastre
export PNG preview
```

No usar canvas para guion/documento/audio.

## PlaybackManifest

Conecta:

```text
segmentId + audioClipId + imageAssetId + start/end seconds
```

Durante reproducción:

- se reproduce audio;
- se resalta texto;
- se muestra/resalta imagen;
- se actualiza statusbar.
