# Generacion, audio manual, pausas y microfonos

Estado: implementado en codigo local JavaFX. Esta nota registra solo cambios verificados en el arbol actual.

## Reintentos compartidos

- `GenerationAttemptPolicy` y `GenerationTaskKind` viven en `application.process`.
- Audio conserva compatibilidad con `tts.maxRetries`: el total efectivo es `maxRetries + 1`.
- El default vigente de audio es 4 intentos totales porque `OperationalSettings.TtsEngineSettings.defaults()` usa `maxRetries = 3`.
- Imagen agrega `ImageGenerationSettings.maxAttempts`; el default es 2 intentos totales.
- `LocalTtsProcessAudioGenerationGateway` usa `GenerationAttemptPolicy.audioMaxAttemptsFromRetries(...)` en ruta segmento y batch XTTS.
- `ComfyUiClient` usa la misma politica para candidato/frame, publica estados de progreso y reintenta fallos temporales.

## Pausas de playback y exportacion

- `PunctuationPausePolicy` es la fuente de verdad para pausas entre `PlaybackCue`.
- `PlaybackTimingPolicy` la usa en reproduccion secuencial.
- `ExportPodcastWavUseCase.exportPlaybackManifest(...)` la usa al concatenar manifest de cues.
- `PlaybackCue` ahora incluye `PlaybackCueKind`.
- `BuildPlaybackManifestUseCase` deriva `PlaybackCueKind` desde `NarrationSegment.type()` para `TITLE`, `HEADING` y `SUBHEADING`.
- Indice estructural especifico: no verificado; el codigo no tiene un metadato separado de indice, por lo que no se inventa una heuristica textual.

## Grabacion manual desde mapa textual

- Menu contextual: `Grabar audio narracion/efecto sonido`.
- Dialogo JavaFX usa `AudioInputDeviceSelector`.
- El texto completo se muestra en `TextArea` grande con estilo `theatre-manual-script-text`; no usa `preview()` truncado salvo fallback cuando no existe texto completo.
- `Dejar de grabar` crea borrador WAV y no aplica snapshot.
- `Asignar audio a intervencion` aplica el borrador como `*-manual.wav`.
- `Escuchar` prioriza borrador; si no existe borrador, reproduce el audio aplicado.
- `Cerrar` se retiro del dialogo; se cierra con X o Escape.

## Selector transversal de microfono

- `AudioInputDeviceSelector` vive en `presentation.components`.
- Usa `AudioInputDevice.systemDefault()` como fallback si no hay dispositivos enumerables.
- `VoiceSampleActions` lo usa para grabar muestras de referencia.
- `VoiceSampleWorkflowCoordinator.startRecording(..., inputDeviceId)` pasa el dispositivo hasta `StartAudioRecordingUseCase.startInDirectory(...)`.

## Imagen IA y trabajos

- `TheatreImageAspectRatio` define `16:9`, `9:16`, `1:1`, `4:3`, `3:4`, `21:9`.
- El default de UI queda en `16:9` y `FHD_1080` conserva `1920x1080`.
- `ComfyUiClient` usa `aspectRatio.promptText()`, `widthFor(...)` y `heightFor(...)`; se elimino el prompt fijo `16:9 composition` para relaciones alternativas.
- `TheatreImageGenerationWorkspaceView` cambia a `Trabajos` al iniciar candidato/frame/lote y actualiza status de job con progreso del motor.
- Los botones visibles de mejora/escalado/pipeline se ocultaron de `Candidatos individuales`; el backend `enhanceTheatreImageCandidate` queda disponible.

## Pruebas agregadas o actualizadas

- `GenerationAttemptPolicyTest`.
- `TheatreImageAspectRatioTest`.
- `PunctuationPausePolicyTest` con texto estructural.
- `ComfyUiClientAspectRatioSourceTest`.
- `TheatreImageGenerationWorkspaceSourceTest`.
- Tests fuente de grabacion manual, voces y candidatos actualizados.
