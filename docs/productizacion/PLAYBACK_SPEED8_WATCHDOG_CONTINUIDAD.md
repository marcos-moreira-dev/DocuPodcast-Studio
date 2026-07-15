# PLAYBACK-SPEED8 — watchdog de continuidad real

## Problema observado

Después de PLAYBACK-SPEED7 los tests quedaban verdes, pero la reproducción seguía deteniéndose tras uno o dos fragmentos. Eso indica que el problema ya no debe tratarse solo como test/manifest, sino como robustez runtime: el reproductor, el timer de JavaFX o el manifest pueden no disparar el avance en el momento correcto.

## Solución

Se agrega un watchdog de reproducción independiente del `Timeline` de JavaFX y del callback de Java Sound.

Cada cue iniciado programa una verificación por duración esperada del WAV/cue. Si al vencerse esa ventana el cursor sigue en la misma unidad activa, el watchdog fuerza la ruta normal de avance con `advanceAfterCompletedCue(...)`.

También se corrige la continuación después de buffer: cuando aparece el siguiente WAV, la app transiciona al `PlaybackCue` exacto con `transitionToCue(nextCue.get())` en vez de volver a resolver por selección/segmento, lo que podía regresar al fragmento original.

## Validación manual

1. Generar chunks.
2. Reproducir desde una oración intermedia.
3. Dejar correr al menos cinco fragmentos.
4. Cambiar entre `1x`, `1.5x` y `1.75x`.
5. Confirmar que la reproducción continúa incluso si la generación sigue activa.
