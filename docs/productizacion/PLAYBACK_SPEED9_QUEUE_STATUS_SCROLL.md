# PLAYBACK-SPEED9 / STATUS-SCROLL1

## Motivo

Tras SPEED6-SPEED8, los tests pasaban pero la experiencia seguía igual: la lectura reproducía el primer chunk y se detenía, o avanzaba uno más y se quedaba. Eso indica que no bastaba con parchear el callback de Java Sound, el `Timeline` o el manifest temprano.

## Decisión técnica

Se separó la continuidad real de lectura en dos capas:

1. `PlaybackRuntimeQueue`: cola viva de cues, derivada del manifest pero no dependiente de la selección actual del documento.
2. Monitor periódico de cue activo: comprueba si el reproductor dejó de sonar, si llegó al final, si superó el tiempo esperado o si quedó estancado.

La idea es que, una vez que el usuario inicia desde una oración, la continuidad ya no vuelva a resolver desde esa misma selección; debe avanzar por la cola ordenada de chunks reproducibles.

## Cambios principales

- Nuevo `PlaybackRuntimeQueue`.
- `advanceAfterCompletedCue(...)` pregunta primero a la cola runtime por el siguiente cue.
- Cada cue iniciado activa `startCueMonitoring(...)`.
- El monitor usa posición local real del WAV, no posición absoluta del manifest.
- Si el player queda detenido o estancado, se fuerza avance al siguiente cue de la cola.
- El status bar ahora usa un `ScrollPane` horizontal para mensajes largos.

## Validación manual recomendada

1. Generar chunks.
2. Iniciar desde una oración intermedia.
3. Dejar avanzar 5-10 fragmentos sin tocar controles.
4. Cambiar a 1.5x y 1.75x mientras suena.
5. Confirmar que la cola avanza y que el status muestra la posición de cola si hay problema.
6. Probar arrastrar horizontalmente el status bar para leer mensajes largos.
