# PLAYBACK-CORE2 — deadline sequencer y status bar sin scrollbar visible

## Problema observado

Los tests podían pasar, el manifest mostraba 26 cues y el siguiente WAV existía, pero la lectura seguía quedándose tras el primer fragmento o tras pocos fragmentos. El diagnóstico visible confirmó que el problema ya no era falta de chunks, sino continuidad runtime.

## Decisión

Dejar de depender únicamente de callbacks de Java Sound, `Timeline` JavaFX o posición de línea. Cada cue/chunk ahora tiene también un deadline calculado desde su duración WAV y velocidad activa. Si ese tiempo vence y el cue sigue activo, el sistema avanza por la misma ruta de continuidad.

## Cambios

- Nuevo `PlaybackCueDeadlineSequencer` en `presentation.shell.workflow`.
- `DocuPodcastShellViewModel` delega el deadline del cue al secuenciador.
- Se mantiene el diagnóstico runtime existente para manifest, cue activo, siguiente cue, WAV y cola.
- El status bar conserva scroll horizontal por rueda/pan, pero oculta la barra dibujada para que no se vea comprimido ni tosco.
- Guardarraíl ajustado para el resolver de imagen del sidebar izquierdo y rail derecho.

## Prueba manual

1. Generar chunks.
2. Reproducir desde selección.
3. Esperar varios fragmentos sin tocar nada.
4. Cambiar a `1.5x` y `1.75x`.
5. Confirmar que la lectura continúa después del cue actual.
6. Usar la rueda sobre el status bar y confirmar desplazamiento horizontal sin scrollbar visible.
