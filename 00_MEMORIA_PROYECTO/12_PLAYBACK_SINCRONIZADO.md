# Playback sincronizado

DocuPodcast debe reproducir audio, texto e imagen de forma coordinada.

## Niveles

### Nivel 1 — por segmento

Mientras suena `SEG-018.wav`, se resalta el segmento `SEG-018` y la imagen asociada.

Este es el MVP.

### Nivel 2 — por oración

Si se generan clips por oración o se crean marcas internas, se puede resaltar una oración.

### Nivel 3 — por palabra/línea

Requiere timestamps precisos o alineación forzada. No es MVP.

## PlaybackManifest

```text
PlaybackManifest
  └── cues[]
       ├── segmentId
       ├── audioClipId
       ├── imageAssetId
       ├── startSeconds
       ├── endSeconds
       └── scriptRange
```

## UI

- Barra inferior de reproducción.
- Play / pause / stop.
- Segmento actual.
- Tiempo actual.
- Resaltado de texto.
- Resaltado de escena en storyboard.

## Regla

El reproductor consume manifests y estado. No debe generar audio ni modificar guion.
