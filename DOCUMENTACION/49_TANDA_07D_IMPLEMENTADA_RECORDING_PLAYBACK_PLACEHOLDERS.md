# Tanda 7D implementada — UI para grabación, voz humana y playback clicable

## Objetivo

Cerrar la línea opcional solicitada antes del TTS real: dejar visible y testeada la preparación de:

- selección de texto/segmento en el workspace de Guion;
- elección futura entre voz IA/TTS y voz humana real;
- preparación de grabación humana asociada al texto;
- preparación de audio para futura transcripción Whisper/STT;
- playback pausable/reanudable desde el segmento/línea clicada.

La tanda no implementa captura real de micrófono ni Whisper. Eso queda como fase posterior deliberada.

## Cambios principales

### Servicios nuevos

- `RecordingApplicationServices`.
- `PlaybackApplicationServices`.
- `PrepareRecordingActionUseCase`.
- `RecordingActionPlan`.

### Shell/ViewModel

`DocuPodcastShellViewModel` ahora expone:

- `selectedScriptSegmentIdProperty()`.
- `playbackCursorProperty()`.
- `selectScriptSegment(...)`.
- `playFromSelectedSegment()`.
- `pausePlayback()`.
- `resumePlayback()`.
- `stopPlayback()`.
- `prepareAiVoiceForSelectedText()`.
- `prepareHumanVoiceForSelectedText()`.
- `prepareSpeechToTextForSelectedText()`.

### Guion

`ScriptWorkspaceView` ahora permite:

- hacer clic en un segmento/tarjeta y convertirlo en selección activa;
- preparar voz IA/TTS para la selección;
- preparar voz humana grabada para la selección;
- preparar audio para Whisper/STT;
- reproducir desde la selección;
- pausar/reanudar/detener el playback simulado;
- resaltar segmento seleccionado y segmento en reproducción.

### Menú y toolbar

Se agregaron acciones visibles en menú/toolbar:

- Voz IA/TTS.
- Grabar humano.
- Whisper/STT.
- Play selección.
- Menú Reproducción con reproducir, pausar, reanudar y detener.

## Alcance consciente

La tanda deja contratos y botones, pero todavía no hace:

- captura real de micrófono;
- importación de WAV humano con picker dedicado;
- envío de audio a Whisper/STT;
- inserción automática del texto transcrito en el editor;
- reproducción real de audio generado;
- seek por palabra exacta.

La granularidad actual es segmento/línea de guion.

## Por qué se hizo antes de TTS real

Porque estas capacidades impactan el modelo de producto: un segmento/rango de texto puede ser leído por IA/TTS, voz humana grabada o audio humano para STT. Tener el contrato visible evita diseñar el TTS real como único camino irreversible.

## Tests agregados

- `PrepareRecordingActionUseCaseTest`.
- `SeekPlaybackUseCaseTest`.
- `ScriptWorkspaceRecordingPlaybackSourceTest`.
- `ToolbarRecordingPlaybackSourceTest`.
