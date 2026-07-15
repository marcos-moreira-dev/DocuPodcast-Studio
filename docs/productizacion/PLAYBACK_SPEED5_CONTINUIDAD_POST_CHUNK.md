# PLAYBACK-SPEED5 — continuidad post chunk y falso final de reproductor

## Objetivo

Corregir el caso reportado donde `Reproducir desde selección`, `Reproducir desde aquí` o el playbar reproducían uno o dos fragmentos y luego no continuaban.

## Causa probable

El reproductor Java Sound podía terminar el WAV real antes de que el reloj de cue (`PlaybackCueClock`) considerara completada la duración del manifest. Esto puede ocurrir cuando la duración persistida/estimada no coincide exactamente con el audio real o cuando hay reinicio por cambio de velocidad.

En ese caso, el reproductor ya no sonaba, pero el timer seguía esperando el reloj lógico y no saltaba al siguiente cue.

## Corrección

- `tickPlayback()` ahora reconoce un final natural cuando el reproductor deja de sonar después de la guardia corta de transición.
- La guardia evita falsos finales durante reinicios de velocidad.
- Si el reproductor realmente terminó y la guardia ya expiró, se avanza al siguiente cue aunque el reloj lógico todavía no haya llegado exactamente al borde final.
- Se conserva `transitionToCue(...)` para detener de forma limpia el WAV anterior e iniciar el siguiente.

## Validación manual

1. Generar chunks de un DOCX largo.
2. Seleccionar una oración.
3. Usar `Reproducir desde selección`.
4. Confirmar que avanza más de dos fragmentos.
5. Cambiar entre `1x`, `1.5x` y `1.75x`.
6. Confirmar que al terminar cada WAV pasa al siguiente sin repetir ni quedarse detenido.
