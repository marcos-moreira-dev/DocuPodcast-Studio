# RF5 — Limpieza residual STT/Whisper

Base: RF4 verde confirmada por el usuario.

## Objetivo

Cerrar residuos de audio-a-texto que sobrevivían como nombres de dominio, mensajes de smoke o referencias operativas. La regla vigente queda reforzada: DocuPodcast Studio no ofrece Whisper, STT, Speech-to-Text ni audio a texto como flujo de producto.

## Cambios

- `VoiceSourceKind` elimina `HUMAN_RECORDING_FOR_TRANSCRIPTION`.
- `VoiceSourceKind.HUMAN_RECORDING` queda como audio del computador asociado al documento, no como fuente de transcripción.
- `PerformanceSpan` valida únicamente `AI_TTS`, `HUMAN_RECORDING` y `UNASSIGNED`.
- El smoke real opt-in deja de imprimir “audio a texto/STT” y habla de transcripción de audio solo como alcance excluido.
- Se agrega guardarraíl RF5 para impedir que `src/main/java` o `scripts` vuelvan a traer `Whisper`, `SpeechToText`, `SPEECH_TO_TEXT`, `allowGpuForStt`, `audio a texto` o `HUMAN_RECORDING_FOR_TRANSCRIPTION`.

## No cambios

- No se elimina la importación/grabación de audio del computador.
- No se elimina el uso de clips locales como audio asociado a fragmentos.
- No se cambian componentes visuales ni CSS.
- No se reintroducen nombres técnicos de motores en la interfaz gráfica normal.

## Validación

- `ResidualWhisperSttCleanupRf5SourceTest`.
- `NoWhisperSttT114Hf2SourceTest` sigue vigente.
- `PerformanceSpanTest` sigue cubriendo audio humano/local como clip, no transcripción.
