# PLAYBACK-SPEED6 — controlador estructural de continuidad

## Motivo

Las tandas SPEED2–SPEED5 mejoraron velocidad, tono y algunos bordes del timer, pero el usuario confirmó que la reproducción seguía deteniéndose después de uno o dos fragmentos. La causa probable ya no debía tratarse como un parche de UI: el avance dependía demasiado de polling (`Timeline`) y de cálculos de reloj lógico, mientras que el reproductor Java Sound conoce con más certeza cuándo terminó realmente un WAV.

## Cambio estructural

Se introduce `PlaybackContinuationController` como dueño de la continuidad runtime:

- cue activo real;
- `PlaybackCueClock`;
- guardia corta de transición al reiniciar/cambiar velocidad;
- marca de final natural emitida por el reproductor;
- resolución de cue activo y siguiente cue.

`DocuPodcastShellViewModel` conserva mensajes, selección, buffer y estado visual, pero ya no concentra las reglas finas de continuidad.

## Callback real del reproductor

`SegmentAudioPlayer` agrega `setOnPlaybackFinished(Consumer<Path>)`.

`JavaSoundSegmentAudioPlayer` invoca ese callback solo cuando el WAV termina naturalmente, no cuando el usuario detiene, cambia de fragmento o se reinicia por velocidad. El callback entrega el `Path` del WAV terminado para que el ViewModel ignore eventos tardíos que ya no correspondan al cue activo.

## Reglas de avance

La reproducción avanza si:

1. el callback del reproductor confirma que terminó el WAV activo;
2. el reproductor ya dejó de sonar después de la guardia de transición;
3. el reloj lógico confirma fin de cue.

Si falta el siguiente WAV y el job sigue corriendo, se conserva el comportamiento de buffer: esperar y continuar cuando exista.

## Validación manual

1. Generar chunks de un documento largo.
2. Seleccionar una oración.
3. Usar `Reproducir desde selección`.
4. Confirmar que avanza varios fragmentos seguidos.
5. Cambiar a `1.5x`, luego `1.75x`, luego `1x`.
6. Confirmar que el final de cada WAV dispara el siguiente cue sin repetir ni detenerse.
