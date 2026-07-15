# PLAYBACK-CORE1 — diagnóstico runtime de reproducción

## Objetivo

La continuidad de lectura sigue deteniéndose aunque los tests pasen. Esta tanda no intenta ocultar el problema con más temporizadores: expone el estado real del transporte de playback para poder ver por qué no avanza.

## Cambios

- Agrega `PlaybackRuntimeDiagnostics`, un snapshot legible del estado activo de reproducción.
- El diagnóstico reporta:
  - manifest y job activo;
  - cantidad de cues/chunks;
  - cue activo y segmento activo;
  - siguiente cue calculado;
  - ruta WAV esperada;
  - si el WAV existe o no;
  - duración del cue;
  - posición local del reproductor;
  - velocidad activa;
  - si el player está sonando o detenido;
  - estado de la cola runtime.
- El status bar desplazable ahora puede mostrar este diagnóstico cuando:
  - un chunk termina y la app intenta avanzar;
  - falta buffer;
  - no existe el WAV esperado;
  - no se puede continuar.

## Validación manual

1. Generar chunks.
2. Reproducir desde selección.
3. Cuando se detenga, leer el status bar desplazable.
4. Reportar especialmente:
   - `activo`;
   - `siguiente`;
   - `WAV existe/no existe`;
   - `pos`;
   - `player`;
   - `Cola de lectura`.
