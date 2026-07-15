# PLAYBACK-SPEED3 — continuidad al cambiar velocidad

## Motivo

Después de `PLAYBACK-SPEED2`, la aceleración con tono más natural funcionaba durante el chunk activo, pero podían quedar dos comportamientos no aceptables para lectura continua:

- en algunos casos un fragmento podía repetirse al inicio o después de una transición;
- si el usuario cambiaba entre `1x`, `1.5x` y `1.75x` durante un chunk, ese chunk sonaba bien, pero al terminar podía no avanzar al siguiente.

## Cambios

- `DocuPodcastShellViewModel.setPlaybackRate(...)` ahora resuelve el cue activo antes de reiniciar el reproductor y resincroniza el reloj de playback desde la posición local real del player.
- El cambio de velocidad vuelve a iniciar `PlaybackCueClock` con `playbackCueClock.start(activeCue, local, rate)` en vez de solo cambiar la tasa del reloj por pared.
- `tickPlayback()` usa `playingCueSegmentId` como fuente de verdad para saber qué unidad empezó realmente a sonar. Esto evita confusiones cuando el cursor absoluto cae justo en frontera entre cues o cuando el manifest se reconstruyó durante generación.
- Si el reproductor interno ya terminó y el reloj ya no bloquea el cue, la transición al siguiente cue se considera válida aunque la posición calculada no haya avanzado exactamente hasta el final.
- El autostart por buffer ya no interpreta cualquier cursor pausado como permiso para arrancar otra vez desde la selección. Solo arranca automáticamente si el cursor está detenido; la recuperación después de un gap sigue pasando por `waitingForBufferedSegmentAfter`.

## Resultado esperado

- Cambiar velocidad durante un chunk no debe cortar la cadena de reproducción.
- Al terminar el chunk acelerado debe arrancar el siguiente WAV si existe.
- Si falta el siguiente WAV y hay job de generación activo, debe esperar buffer y continuar después.
- No debe repetirse el mismo fragmento por autostart de buffer o por ambigüedad de cursor en fronteras.

## Validación manual

1. Generar chunks de audio.
2. Reproducir desde selección.
3. Cambiar a `1.5x` durante el primer chunk.
4. Confirmar que al terminar continúa al siguiente.
5. Cambiar a `1.75x` en otro chunk y volver a `1x`.
6. Confirmar que no repite el chunk activo.
7. Si se alcanza un chunk pendiente, confirmar que espera buffer y luego continúa.
