# Playback sincronizado

DocuPodcast debe poder reproducir audio y señalar qué texto/imagen corresponde.

## MVP

Seguimiento por segmento:

1. Suena `SEG-018.wav`.
2. Se resalta `SEG-018` en el guion.
3. Se resalta la escena `SEG-018` en storyboard.
4. Se muestra la imagen asociada.
5. La barra inferior indica tiempo y segmento.

## Futuro

- seguimiento por oración;
- seguimiento por rango de texto;
- alineación palabra por palabra si existe manifest.

## PlaybackManifest

```text
PlaybackManifest
  └── cues[]

PlaybackCue
  ├── segmentId
  ├── audioClipId
  ├── imageAssetId
  ├── startSeconds
  ├── endSeconds
  └── scriptRange opcional
```

## Regla de arquitectura

El reproductor no debe inventar relaciones. Debe usar manifests generados desde el guion, audio y storyboard.
