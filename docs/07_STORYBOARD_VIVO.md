# Storyboard vivo

## Definición

Storyboard vivo = imágenes del usuario asociadas a segmentos del guion, reproducidas junto con el audio generado.

## Alcance inicial

- Una imagen por segmento.
- Preview de escena.
- Resaltado de escena activa durante playback.
- Sin película automática.
- Sin animación compleja.

## Modelo conceptual

```text
NarrationSegment
  → StoryboardBinding
  → MediaAsset/Image
  → PlaybackCue
```

## Responsabilidad del usuario

El usuario decide qué imagen corresponde a cada segmento. La app no intenta inferirlo automáticamente.
