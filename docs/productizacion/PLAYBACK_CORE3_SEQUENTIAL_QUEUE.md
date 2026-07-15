# PLAYBACK-CORE3 — cola secuencial exacta de cues

## Problema observado

Aunque el manifest mostraba `cues 26`, `siguiente existe` y `WAV existe`, la lectura seguía deteniéndose o repitiendo el comportamiento de reproducir solo el primer fragmento. El diagnóstico visible confirmó que el problema no era falta de WAV ni manifest parcial.

## Hallazgo técnico

La ruta antigua seguía resolviendo el cue activo desde `PlaybackCursor` y `activeUnitId`. Cuando varios cues pertenecen al mismo `segmentId` —por ejemplo `SEG-002-U001` y `SEG-002-U002`— la resolución podía favorecer el cue activo anterior por coincidir con el mismo segmento, en vez de abrir exactamente el cue siguiente.

## Solución

Se agregó `PlaybackSequentialQueueDriver`, una cola secuencial que:

- conserva el orden del manifest desde el cue inicial;
- programa el siguiente cue por duración real y velocidad activa;
- no vuelve a resolver el siguiente audio desde la selección del documento;
- inicia el cue exacto con `playExactCue(cue, 0.0)`;
- deja el `Timeline` solo como soporte de sincronización visual, no como dueño de la continuidad.

## Contrato operativo

- `Reproducir desde selección` arranca una cola desde el cue elegido.
- El cue siguiente se toma por identidad exacta de unidad (`unitId`), no por segmento general.
- Si el siguiente cue está dentro del mismo párrafo/segmento, debe reproducirse igual sin repetir el cue anterior.
- Los botones `1x`, `1.5x` y `1.75x` reprograman la cola según la velocidad activa.

## Validación manual sugerida

1. Abrir un proyecto con varios chunks por párrafo.
2. Reproducir desde una oración intermedia.
3. Confirmar avance de `SEG-002-U001` a `SEG-002-U002`, no repetición de `SEG-002-U001`.
4. Cambiar a `1.5x` o `1.75x` y confirmar que la cola sigue avanzando.
