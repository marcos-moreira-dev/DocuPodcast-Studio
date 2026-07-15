# Tanda 12 — Playback sincronizado real

Esta tanda conecta tres piezas ya existentes del proyecto:

```text
Guion narrable
+ Audio generado por segmentos
+ Storyboard con imágenes asociadas
= Playback sincronizado por cues
```

## Implementado

- `PlaybackManifest`: manifiesto ordenado de reproducción.
- `PlaybackCue`: une segmento, tiempo de inicio/fin, audio relativo e imagen opcional.
- `BuildPlaybackManifestUseCase`: construye cues desde `NarrationScriptDocument`, `AudioJobSnapshot` y `StoryboardDocument`.
- `AdvancePlaybackCursorUseCase`: avanza el cursor sin depender de JavaFX.
- `SegmentAudioPlayer`: puerto para reproducir audio por segmento.
- `JavaSoundSegmentAudioPlayer`: implementación WAV-first usando `javax.sound.sampled`.
- Integración del playback en `DocuPodcastShellViewModel`.
- Resaltado de segmento activo en Guion.
- Resaltado de escena activa en Storyboard.
- Vista de cues en el workspace Audio.

## Alcance real

El playback ahora usa el manifest de segmentos completados y puede reproducir WAV por segmento cuando el entorno Java Sound lo permite. Si el audio local no está disponible o falla, la sincronización visual sigue mostrando cursor/cues para no bloquear la operación.

## Pendiente

- UI de reproductor más refinada.
- Control fino de volumen/velocidad.
- Alineación por oración/palabra.
- Reproducción de MP3 si el runtime lo soporta.
- Sincronización con voz humana grabada y STT real.
