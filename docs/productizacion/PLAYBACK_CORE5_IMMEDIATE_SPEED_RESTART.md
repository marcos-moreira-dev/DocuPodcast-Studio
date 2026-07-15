# PLAYBACK-CORE5 — reinicio inmediato al cambiar velocidad

## Motivo

El usuario detectó que, al pulsar `1x`, `1.5x` o `1.75x` durante un fragmento, la app podía dejar un silencio incómodo antes de reproducir el mismo chunk con la nueva velocidad. El comportamiento esperado es más simple: el botón de velocidad debe reiniciar inmediatamente el chunk activo con la nueva velocidad.

## Decisión

Cuando hay un cue activo y el usuario cambia velocidad:

1. Se detiene el reproductor actual.
2. Se limpia el reloj lógico del cue.
3. Se configura la velocidad nueva.
4. Se reposiciona el cursor al inicio del cue activo.
5. Se reproduce exactamente ese mismo `unitId` desde `0.0`.
6. La cola secuencial se reprograma desde el inicio del chunk.

Esto evita esperar la duración restante del chunk anterior y elimina el silencio que parecía simular que el audio antiguo seguía avanzando.

## Alcance

No cambia la generación TTS ni la cola completa. Solo cambia la semántica de los botones de velocidad mientras hay reproducción activa.
