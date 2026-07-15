# Tanda 7C implementada — Recuperación operativa de jobs persistidos y roadmap de voz humana

## Estado

Implementada sobre la base de la Tanda 7B.

La aplicación ya tenía generación mock y persistencia temprana de jobs. Esta tanda agrega recuperación operativa: los jobs persistidos se pueden inspeccionar con más detalle y el último job reanudable puede continuarse sin regenerar segmentos que ya tenían WAV completado.

## Objetivo técnico

La Tanda 7C prepara la transición hacia el TTS real. Antes de conectar XTTS/Piper/Python, el flujo de recuperación debe funcionar con el motor mock:

```text
Guion narrable
→ JOB persistido
→ segments-status.json
→ completar algunos WAV
→ cancelar / fallar / interrumpir
→ reabrir
→ continuar pendientes/fallidos/cancelados
→ conservar WAV completados
```

## Implementación principal

### Nuevos casos de uso

```text
LoadPersistedAudioJobUseCase
ResumePersistedAudioJobUseCase
AudioJobRecoverySummary
```

### Gateway ampliado

`AudioGenerationGateway` ahora tiene:

```java
String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer);
```

`MockAudioGenerationGateway` implementa ese contrato. Al reanudar:

- conserva segmentos `COMPLETED`;
- no reescribe sus WAV existentes;
- regenera segmentos `PENDING`, `FAILED`, `CANCELLED` o `GENERATING` encontrados en un snapshot persistido;
- reconstruye `audio-manifest.json` y `final/podcast-mock.wav`;
- registra eventos `job_resuming` y `mock_audio_resumed_completed` en `generation-log.jsonl`.

### UI de Audio

`AudioWorkspaceView` ahora muestra:

- historial persistido de jobs;
- detalle del job reanudable o más reciente;
- botón **Continuar último reanudable**;
- lista de segmentos con estado, intentos, audio relativo y error cuando exista.

## Infraestructura preliminar para futuras capacidades pedidas

El usuario pidió dejar documentado y parcialmente preparado lo siguiente:

1. asignar a una selección de texto una voz IA/TTS o una voz humana real;
2. permitir que esa selección también esté relacionada con una imagen;
3. preparar la infraestructura para un botón o flujo de **grabar audio**;
4. permitir audio humano asociado directamente a texto;
5. permitir audio humano como fuente para Whisper/STT, para insertar texto en el workspace tipo Word;
6. pausar reproducción de la obra/documento y reanudar desde cualquier línea/segmento clicado.

La Tanda 7C no implementa la grabación real ni Whisper, pero deja contratos de dominio preliminares:

```text
ScriptTextRange
VoiceSourceKind
PerformanceSpan
RecordingPurpose
AudioRecordingReference
AudioRecordingGateway
PlaybackCue
PlaybackCursor
SeekPlaybackUseCase
```

Esto permite modelar desde ya:

```text
rango de texto
→ voz IA o voz humana
→ estilo/personaje opcional
→ imagen asociada opcional
→ audio grabado opcional
→ futuro cue de reproducción
```

## Regla de producto documentada

La voz asignada a texto debe poder provenir de dos familias:

```text
AI_TTS           = voz generada por motor IA/TTS
HUMAN_RECORDING  = voz humana grabada o importada
```

Y una grabación humana puede usarse para dos propósitos distintos:

```text
voz final asociada a texto
muestra/fuente para transcripción o entrenamiento/configuración futura
```

## Limitaciones conscientes

Aún no existe:

- grabación real desde micrófono;
- importador de audio humano desde UI;
- integración Whisper/STT;
- selector visual de rango de texto para `PerformanceSpan`;
- selector IA vs humano en el workspace Guion;
- reproducción real con cursor clicable;
- reanudación desde cualquier línea con audio real.

Estas capacidades quedan planificadas para tandas posteriores.

## Tests asociados

```text
MockAudioGenerationGatewayResumeTest
PerformanceSpanTest
AudioRecordingReferenceTest
PlaybackCursorTest
```

Cubren:

- reanudar un job persistido sin sobrescribir WAV completados;
- resumen de recuperación;
- asignación de voz IA a rango textual;
- requerimiento de audio humano para voz humana;
- grabación marcada para STT/Whisper;
- pausa/reanudación/seek de cursor de playback.
