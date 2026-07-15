# Memoria — Tanda 8 Gateway TTS real

La Tanda 8 implementa el primer punto de integración real con motores de voz locales.

Decisión clave: no acoplar JavaFX a XTTS, Piper, Python ni ONNX directamente. En su lugar, `AudioGenerationGateway` tiene una implementación por proceso local: `LocalTtsProcessAudioGenerationGateway`.

Si el comando TTS está configurado, la app usa motor real; si no, usa mock. Esta decisión mantiene instalabilidad y permite probar la app sin modelos pesados.

Variables/propiedades importantes:

- `DOCUPODCAST_TTS_COMMAND`
- `DOCUPODCAST_TTS_DISPLAY_NAME`
- `DOCUPODCAST_TTS_LANGUAGE`
- `DOCUPODCAST_TTS_VOICE`
- `DOCUPODCAST_TTS_TIMEOUT_SECONDS`
- `docupodcast.tts.command`
- `docupodcast.tts.displayName`
- `docupodcast.tts.language`
- `docupodcast.tts.voice`
- `docupodcast.tts.timeoutSeconds`

Queda pendiente empaquetar/seleccionar un worker concreto de alta calidad y exponer configuración visual en Voice Library/Engine Diagnostics.
