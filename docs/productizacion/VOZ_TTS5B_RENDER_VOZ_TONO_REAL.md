# VOZ-TTS5B — render TTS usa voz y tono real

## Objetivo

Conectar lo que Documento ya muestra de forma honesta tras VOZ-TTS5A con la generación real de audio. La asignación por fragmento debe viajar hasta el proceso TTS: voz seleccionada, tono registrado y muestra de referencia correspondiente.

## Cambios

- `AudioGenerationRequest` conserva la `VoiceLibrary` del proyecto cuando se genera audio desde Documento.
- Cada `AudioGenerationUnit` puede resolver su muestra de referencia mediante `referenceSamplePathFor(...)`.
- El tono se interpreta desde `AudioGenerationUnit.performanceStyleId()` usando `VoiceReferenceTone.fromLayerTargetId(...)`.
- Si el tono pedido existe para la voz, se usa esa muestra.
- Si el tono pedido no existe pero hay Neutral, se usa Neutral como fallback.
- La ruta de muestra se valida dentro de la carpeta del proyecto cuando es relativa.
- `LocalTtsProcessAudioGenerationGateway` pasa la muestra resuelta al comando TTS.
- `LocalTtsProcessConfiguration` expone el overload público de `commandFor(..., Path speakerWav)` usado por jobs reales, no solo por pruebas de voz.
- El comando conserva placeholders `{speakerWav}`, `{referenceSample}` y `{voiceSample}`, y también sobreescribe `-SpeakerWav`/`--speaker-wav` cuando la plantilla lo trae explícito.

## Alcance

Esta tanda no descarga ni repara Voz IA avanzada. Tampoco cambia playback, UI de Voces, UI de Documento ni la cola de audio.

## Regla de producto

Documento no debe prometer una emoción falsa: VOZ-TTS5A ya filtra lo visible. VOZ-TTS5B completa el contrato usando en el render la muestra real que corresponde a esa voz/tono. La Voz local simple puede ignorar muestras si su plantilla no usa speaker WAV.

## Validación

- Source test: `VoiceTts5BRenderUsesReferenceSampleSourceTest`.
- Test focal de aplicación: `AudioGenerationRequestVoiceReferenceSampleTest`.
- Compilación focal sin JavaFX: dominio + aplicación + infraestructura/audio + infraestructura/json con stubs JUnit.
- Validación completa pendiente en Windows: `scripts\\99-diagnostico-completo.bat`.
