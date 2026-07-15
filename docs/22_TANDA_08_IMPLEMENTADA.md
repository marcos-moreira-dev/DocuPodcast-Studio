# Tanda 8 implementada — Gateway TTS real

Se agregó `LocalTtsProcessAudioGenerationGateway`, configurado por `DOCUPODCAST_TTS_COMMAND` o `-Ddocupodcast.tts.command`.

La app sigue usando mock si no hay motor real configurado. Si hay comando TTS, genera WAVs reales por segmento mediante proceso local.
