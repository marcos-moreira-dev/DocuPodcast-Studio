# Memoria — Tanda 7D

La Tanda 7D implementa placeholders operativos para selección de texto, elección de fuente de voz y playback clicable.

Decisiones fijadas:

- Una selección del guion se representa por segmento completo en esta fase; rangos internos ya existen como `ScriptTextRange` para futuro.
- La voz puede ser `AI_TTS`, `HUMAN_RECORDING` o `HUMAN_RECORDING_FOR_TRANSCRIPTION` a nivel conceptual.
- La UI muestra botones para voz IA/TTS, voz humana y Whisper/STT, pero no promete implementación real todavía.
- El playback simulado usa `PlaybackCursor` y permite pausar/reanudar/detener desde el segmento seleccionado.
- La grabación real de micrófono y Whisper quedan para una tanda futura.

Siguiente recomendación: Tanda 8 — Gateway TTS real, salvo que se quiera implementar primero una micro-tanda de Voice Library mínima.
