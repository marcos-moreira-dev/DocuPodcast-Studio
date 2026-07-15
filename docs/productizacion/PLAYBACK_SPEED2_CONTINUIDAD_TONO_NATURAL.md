# PLAYBACK-SPEED2 — continuidad de lectura y velocidad con tono natural

## Motivo

Después de `PLAYBACK-SPEED1`, los botones `1x`, `1.5x` y `1.75x` ya existían en la playbar, pero la aceleración usaba frecuencia efectiva de salida. Eso hacía que la voz se acelerara con cambio de tono. Además, durante pruebas reales se detectó que al reproducir desde selección podía percibirse como una reproducción de una sola oración, y que cambiar velocidad durante un fragmento podía dejar la continuidad más frágil al pasar al siguiente chunk.

## Cambios

- `JavaSoundSegmentAudioPlayer` deja de acelerar elevando el sample rate de salida.
- Se agrega `PcmTimeStretchProcessor`, un procesador PCM 16-bit con overlap-add y búsqueda de correlación local para acelerar `1.5x` y `1.75x` preservando mejor el tono.
- El reproductor mantiene el formato de salida original del WAV y acorta la señal procesada antes de enviarla a Java Sound.
- `DocuPodcastShellViewModel.setPlaybackRate(...)` mantiene vivo el `playbackTimer` cuando el cursor sigue en reproducción.
- Al completar un cue, el avance al siguiente chunk ahora valida explícitamente si `playCueForCursor(nextCursor)` arrancó bien; si falta buffer y hay job activo, espera; si no, muestra error humano.
- La acción primaria de selección pasa a `Reproducir desde selección`, para dejar claro que la selección es el punto de inicio y no una reproducción limitada a una sola oración.
- El botón lateral queda como `Reproducir fragmento (solo este)` para diferenciar la acción corta de la reproducción continua.
- `FloatingReadingControlBar` conserva el contrato de ancho máximo `820` para no interferir con scrollbars.

## Validación esperada

- Diagnóstico completo verde.
- Reproducir desde selección continúa con las siguientes oraciones/chunks disponibles.
- Cambiar entre `1x`, `1.5x` y `1.75x` durante un chunk no detiene la reproducción del siguiente.
- La voz rápida suena más natural que la aceleración por sample rate.
- Si la reproducción rápida alcanza un WAV todavía no generado, se conserva la espera de buffer.

## Límite consciente

El algoritmo integrado es una solución local y sin dependencias externas para chunks de lectura. No sustituye a un motor DSP profesional dedicado, pero evita el cambio de tono evidente de la primera implementación y mantiene la aplicación portable.
