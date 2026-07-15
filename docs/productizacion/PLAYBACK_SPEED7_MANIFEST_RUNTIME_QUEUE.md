# PLAYBACK-SPEED7 — manifest runtime y cola reproducible

## Problema observado

Aunque los chunks WAV estaban generados, la reproducción desde selección seguía deteniéndose después del primer fragmento o, a veces, después de uno adicional. Las tandas anteriores corrigieron reloj, callback del reproductor y cambios de velocidad, pero el síntoma persistía.

## Diagnóstico técnico

El riesgo principal ya no era solo el timer. La reproducción podía arrancar con un `PlaybackManifest` temprano o parcial, creado cuando solo había uno o pocos chunks disponibles. Si el manifest activo quedaba obsoleto, el reproductor ejecutaba correctamente el WAV actual, pero al terminar no encontraba un siguiente cue confiable.

## Cambios

- Se agrega `PlayableAudioJobSelector` para escoger el job persistido más útil para playback, priorizando:
  - chunks completados que coinciden con el script actual;
  - cantidad total de audio reproducible;
  - job activo como desempate;
  - recencia.
- `rebuildPlaybackManifestFromLatestJob()` usa ese selector en vez de depender del primer snapshot o de un manifest temprano.
- Toda acción de reproducción importante reconstruye manifest en runtime antes de iniciar.
- Al terminar un cue, `advanceAfterCompletedCue(...)` vuelve a reconstruir manifest antes de buscar el siguiente fragmento.
- Se agrega fallback `nextCueAfterCompletedSegmentFallback(...)` para continuar si el manifest fresco no encuentra el cue por unidad pero sí puede resolver el siguiente por segmento/posición.
- Los mensajes de estado incluyen una etiqueta de manifest como `Manifest JOB-...: N chunks listos` para confirmar visualmente si la app está trabajando con una cola amplia o con un manifest parcial.

## Qué no se tocó

- No se cambió el estilo de títulos/subtítulos Word.
- No se tocó la generación TTS.
- No se cambió el time-stretch de velocidad.

## Prueba manual esperada

1. Generar chunks de un documento largo.
2. Esperar a que existan muchos WAV completados.
3. Seleccionar una oración y pulsar `Reproducir desde selección`.
4. Confirmar que el estado muestra un manifest con muchos chunks listos.
5. Dejar reproducir varios fragmentos seguidos sin intervención.
6. Cambiar a 1.5x o 1.75x y confirmar continuidad.
