# Memoria — Tanda 7C

La Tanda 7C implementa recuperación operativa de jobs persistidos y documenta infraestructura preliminar para voz humana, selección de texto, grabación y playback clicable.

## Lo implementado

- `AudioGenerationGateway.resume(...)`.
- `ResumePersistedAudioJobUseCase`.
- `LoadPersistedAudioJobUseCase`.
- `AudioJobRecoverySummary`.
- `MockAudioGenerationGateway` con resume real sobre snapshots.
- `AudioWorkspaceView` con botón **Continuar último reanudable**.
- detalle persistido por segmentos.

## Lo preparado para futuro

- `ScriptTextRange`.
- `VoiceSourceKind`.
- `PerformanceSpan`.
- `RecordingPurpose`.
- `AudioRecordingReference`.
- `AudioRecordingGateway`.
- `PlaybackCue`.
- `PlaybackCursor`.
- `SeekPlaybackUseCase`.

## Decisión

Antes de conectar TTS real, debe existir recuperación confiable. El motor real no debe ser el primer sitio donde probemos reintentos, cancelación o reanudación.

## Pendiente explícito

Queda pendiente que el usuario pueda seleccionar un rango de texto y elegir:

- voz IA/TTS;
- voz humana grabada/importada;
- estilo/personaje;
- imagen asociada;
- audio humano para reproducción directa;
- audio humano para Whisper/STT.

También queda pendiente poder pausar la reproducción total y reanudar desde una línea/segmento clicado.
