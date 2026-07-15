# PLAYBACK-HF8R — cola por duración real del WAV

## Objetivo

Corregir el caso en el que varios chunks se pisan o el Documento salta al siguiente fragmento antes de que el WAV actual termine de sonar.

HF7 corrigió el reloj interno de `JavaSoundSegmentAudioPlayer`, pero todavía quedaba una fuente de error: el job TTS guardaba duración estimada por texto. HF8R mueve la fuente de verdad al archivo WAV real.

## Cambios principales

### 1. Duración real del WAV

Se agrega `WavAudioDurationProbe`, que lee los chunks `fmt ` y `data` del WAV y calcula duración con `dataSize / byteRate`.

Uso aplicado:

- `LocalTtsProcessAudioGenerationGateway` mide `durationProbe.durationSeconds(outputFile)` al terminar cada WAV.
- Si un proceso externo devuelve un archivo inválido o sin duración medible, el segmento no se marca como correcto.
- `AudioJobFileRepository` repara en memoria snapshots antiguos recalculando duración desde el WAV persistido cuando lista/carga jobs.

### 2. Cola/bloqueo secuencial por duración

Se agrega `PlaybackCueClock`, un reloj de pared por cue activo.

`DocuPodcastShellViewModel` ahora:

- arranca el clock cuando llama al reproductor interno;
- pausa/reanuda/detiene el clock junto con el transporte;
- no permite playback bufferizado nuevo si el clock indica que el cue actual sigue dentro de su duración;
- no avanza al siguiente cue mientras `PlaybackCueClock.completed(...)` no confirme que la duración real ya se cumplió.

### 3. Reuso de WAV existente

Cuando se reconstruye el manifest desde jobs persistidos, el repositorio vuelve a medir los WAV existentes. Esto ayuda a corregir jobs creados antes de HF8R que quedaron con duración estimada.

## Alcance deliberadamente no incluido

- No cambia la UX visible de Documento.
- No cambia la estructura JSON del proyecto.
- No cambia la descarga de Voz IA avanzada.
- No ejecuta descarga real de modelos en este entorno.

## Nota sobre Voz IA avanzada

La descarga de Voz IA avanzada sigue siendo un smoke manual pendiente en Windows: hay que verificar que el asistente descargue el modelo, deje los archivos en `models/tts/xtts`, pase inspección local y genere un WAV reproducible. HF8R solo conserva el guardarraíl de que la descarga sea explícita y no silenciosa.

## Validación recomendada

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Con Voz local simple:
   - generar audio;
   - esperar a que haya varios chunks listos;
   - pulsar **Escuchar documento**;
   - confirmar que el fragmento 2 no pisa al fragmento 1.
3. Probar **Siguiente fragmento** y **Fragmento anterior**.
4. Cerrar/reabrir proyecto y probar **Reproducir desde aquí** sobre un WAV ya generado.
5. En Configuración, probar manualmente la descarga/preparación de Voz IA avanzada y registrar si descarga, verifica y permite generar una prueba de voz.

## Guardarraíles

- `PlaybackDurationQueueHf8RSourceTest`
- `WavAudioDurationProbeTest`
- `AudioJobFileRepositoryDurationRepairTest`

