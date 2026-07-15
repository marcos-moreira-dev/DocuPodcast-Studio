# Memoria — Tanda 12 Playback sincronizado

La Tanda 12 implementa el primer playback sincronizado real por segmento. El sistema construye un `PlaybackManifest` a partir del guion, los jobs de audio persistidos y el storyboard. Cada cue conecta:

```text
segmentId
startSeconds/endSeconds
audioRelativePath
imageAssetId opcional
title
```

El usuario puede hacer clic en un segmento del guion o escena del storyboard, reproducir desde allí, pausar, reanudar y detener. La UI resalta el segmento activo y la escena activa. La reproducción de WAV usa Java Sound mediante `JavaSoundSegmentAudioPlayer`, detrás del puerto `SegmentAudioPlayer`.

Esta tanda prepara la futura reproducción completa de obras/documentos largos con pausa y reanudación desde cualquier línea/segmento clicado.
