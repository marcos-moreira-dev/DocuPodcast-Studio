# PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

## Contexto

Después de PLAYBACK-CORE3, la reproducción continua mejoró, pero seguía mostrando comportamiento inconsistente al cambiar velocidad, pausar/reanudar o reproducir mientras todavía se generaban chunks.

El diagnóstico visible confirmó que el manifest, el siguiente cue y el WAV existían. Por tanto, el problema ya no era falta de audio ni manifest parcial, sino coordinación entre:

- cola secuencial;
- Java Sound;
- cambio de velocidad;
- pausa/reanudación;
- avance por deadline calculado.

## Decisión

La cola secuencial sigue siendo la dueña del orden de lectura, pero ya no debe avanzar por una estimación agresiva de duración.

PLAYBACK-CORE4 usa esta prioridad:

1. final real del reproductor (`setOnPlaybackFinished`);
2. cola exacta por `unitId`;
3. watchdog conservador que solo avanza si el reproductor ya no está sonando.

Esto evita cortar la última parte de una frase cuando el tiempo calculado no coincide exactamente con lo que Java Sound todavía está reproduciendo.

## Cambios técnicos

- `DocuPodcastShellViewModel` registra `setOnPlaybackFinished(this::handlePlaybackFinishedOnFxThread)`.
- `advanceAfterPlayerFinished(...)` usa el cue activo de `PlaybackSequentialQueueDriver` antes de resolver por cursor.
- `PlaybackSequentialQueueDriver` agrega:
  - `BooleanSupplier playerPlaying`;
  - `activeCue()`;
  - `advanceAfterCurrentCueFinished()`;
  - watchdog conservador que reintenta si el reproductor sigue sonando.
- Al cambiar velocidad, la cola reprograma el watchdog sin cortar la reproducción actual.

## Smoke manual

1. Generar chunks o reproducir mientras se generan.
2. Reproducir desde selección.
3. Verificar que no corte el final de la frase actual.
4. Cambiar a 1.5x durante un chunk.
5. Verificar que continúa al siguiente cue.
6. Pausar y reanudar.
7. Volver a 1x.
8. Confirmar que la cola sigue avanzando sin repetir el cue anterior.
