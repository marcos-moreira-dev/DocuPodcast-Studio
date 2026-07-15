# Tanda 10 — Voice Library implementada

## Estado

Implementada.

Esta tanda agrega una primera biblioteca de voces real para DocuPodcast Studio. La meta no es clonar voces ni grabar audio todavía, sino crear el dominio, persistencia, servicios y workspace necesarios para que el guion pueda asignar personaje, voz y estilo por segmento.

## Implementado

- Dominio de voz:
  - `VoiceLibrary`
  - `VoiceProfile`
  - `VoiceProfileType`
  - `VoiceEngineType`
  - `VoiceQualityPreset`
  - `CharacterProfile`
  - `PerformanceStyle`
- Biblioteca predeterminada:
  - `VOC-NARRATOR`
  - `VOC-OWN-PLACEHOLDER`
  - `CHR-NARRATOR`
  - `STY-NEUTRAL`
  - `STY-SERIOUS`
  - `STY-DRAMATIC`
- Servicios de aplicación:
  - `CreateDefaultVoiceLibraryUseCase`
  - `ValidateVoiceLibraryUseCase`
  - `AssignVoiceToSegmentUseCase`
  - `MaterializeVoiceLibraryUseCase`
- Persistencia:
  - sección `voiceLibrary` dentro de `.docupodcast.json`
  - snapshot `voices/voice-library.json`
  - asset relativo `VOICE-LIBRARY-001`
- UI:
  - `VoiceLibraryWorkspaceView`
  - menú `Voz`
  - toolbar `Voces`
  - asignación de personaje/voz/estilo al segmento seleccionado
- Tests de dominio, aplicación, infraestructura, JSON y fuente UI.

## Decisiones

La biblioteca de voces es un artefacto del proyecto, no una configuración global aislada. Esto permite que cada obra, documento o guion tenga sus personajes, voces y estilos asociados.

La voz propia y las voces autorizadas quedan modeladas pero no se graban todavía. Toda voz autorizada/importada con muestra exige nota de consentimiento.

Los estilos emocionales se guardan como intención. El resultado real depende del motor TTS y de si el motor soporta estilo, referencia emocional o muestras adecuadas.

## Relación con TTS

La Tanda 8 y 9 dejaron el gateway TTS real por proceso local. La Tanda 10 no cambia el motor: agrega la capa semántica para que el guion tenga voz/personaje/estilo. La conexión fina entre `VoiceProfile` y argumentos del worker TTS se profundizará cuando se conecten perfiles reales de XTTS/Piper.

## Limitaciones conscientes

- No hay grabación real de micrófono.
- No hay importador de muestras de voz todavía.
- No hay clonación/estilo emocional real.
- No hay editor avanzado de voces.
- No hay Voice Library global persistida fuera del proyecto.
- No se genera audio humano real.

## Próximo paso recomendado

Tanda 11 — Storyboard básico.

También puede insertarse una Tanda 10B opcional para importar muestras de voz y registrar assets `VOICE_SAMPLE` antes del storyboard.
