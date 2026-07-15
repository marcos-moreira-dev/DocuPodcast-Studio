# Estrategia — Tanda 12 Playback sincronizado

Esta tanda convierte el playback de placeholder en una capacidad real por segmento. La unidad principal sigue siendo `NarrationSegment`; no se intenta hacer alineación palabra por palabra todavía.

## Regla de diseño

```text
Segmento del guion = unidad mínima estable para audio, imagen y reproducción.
```

## Por qué se hace antes de exportaciones

Antes de exportar podcast/storyboard vivo, la aplicación debe saber relacionar texto, audio e imagen de forma reproducible. `PlaybackManifest` es la base para exportar paquetes y futuros videos simples.

## Guardarraíl

El playback no debe depender de una captura visual ni de un estado efímero del workspace. Debe construirse desde datos persistidos: guion, job de audio y storyboard.
