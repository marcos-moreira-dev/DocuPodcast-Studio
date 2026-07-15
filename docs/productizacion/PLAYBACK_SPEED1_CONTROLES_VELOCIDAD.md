# PLAYBACK-SPEED1 — controles de velocidad en playbar

## Motivo

El lector ya reproduce archivos WAV por fragmento/chunk mientras la generación puede seguir en segundo plano. Para estudiar documentos largos, el usuario necesita acelerar la reproducción de los chunks ya generados sin detener la preparación del resto del audio.

## Cambios implementados

- La playbar del documento agrega tres botones de velocidad: `1x`, `1.5x` y `1.75x`.
- Los botones se habilitan cuando existe un fragmento/cue activo o preparado en playback.
- `1x` vuelve a la velocidad normal.
- `1.5x` y `1.75x` mantienen la lectura rápida para el chunk actual y los chunks siguientes.
- Si la lectura rápida alcanza un fragmento que todavía no fue generado, se conserva el comportamiento existente de buffer: DocuPodcast espera el siguiente WAV y continúa cuando esté disponible.
- `SegmentAudioPlayer` expone `setPlaybackRate(double)` y `playbackRate()`.
- `JavaSoundSegmentAudioPlayer` reproduce WAV a mayor velocidad abriendo la salida Java Sound con frecuencia efectiva superior. Es una aceleración simple de reproducción; no promete conservación avanzada de tono.
- `PlaybackCueClock` queda consciente de velocidad para que la sincronización visual avance al mismo ritmo que el audio acelerado.

## Regla de producto

La velocidad pertenece al playback, no a la generación. No acelera el motor de voz ni consume GPU por sí sola: solo reproduce más rápido los WAV ya generados. La generación puede quedar atrás y el lector debe esperar el buffer sin romper la sesión.

## Validación esperada

1. Generar o reusar audio de un documento.
2. Reproducir desde un fragmento.
3. Cambiar a `1.5x` y confirmar que el chunk actual avanza más rápido.
4. Cambiar a `1.75x` y confirmar que la velocidad se mantiene en el siguiente chunk.
5. Cambiar a `1x` y confirmar retorno a velocidad normal.
6. Durante generación activa, si el playback alcanza un chunk no listo, debe esperar y continuar cuando el WAV aparezca.

## Pendiente relacionado

- `MOTOR-PERF1` debe revisar uso real de GPU/concurrencia para motores de voz. Esta tanda no cambia el motor TTS.
- `PLAYBACK-HF9` debe cerrar robustez completa de WAV persistido, reanudación y reproducción desde aquí.
