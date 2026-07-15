# Tanda 10B opcional — Importación de muestras de voz y assets VOICE_SAMPLE

## Por qué se agrega

Tras implementar Voice Library, la app ya modela voces prediseñadas, voz propia, voces autorizadas, personajes y estilos. Aún falta un puente operativo para importar muestras reales de voz y registrarlas como assets portables.

Esta tanda puede hacerse antes o después del storyboard. Conviene hacerla antes si se quiere probar voces reales con el gateway TTS.

## Alcance

- Botón `Importar muestra de voz` en Voice Library.
- FileChooser para WAV/MP3/FLAC, con validación básica.
- Copia del archivo a `voices/samples/`.
- Registro como `ProjectAssetKind.VOICE_SAMPLE`.
- Asociación del asset a un `VoiceProfile` existente o nuevo.
- Campo de nota de consentimiento para voces autorizadas/importadas.
- Validación: las voces autorizadas con muestra requieren consentimiento.
- Documentación de que no se debe usar voz de terceros sin permiso.

## No alcance

- Grabación desde micrófono.
- Limpieza de ruido.
- Entrenamiento/clonación real.
- Whisper/STT.
- Emoción garantizada.

## Tests esperados

- `ImportVoiceSampleUseCaseTest`.
- `VoiceSampleAssetPolicyTest`.
- `VoiceLibraryConsentValidationTest`.
- `VoiceLibraryWorkspaceImportSampleSourceTest`.

## Resultado

Una voz propia/autorizada puede tener una muestra local persistida como asset relativo, lista para que el worker TTS real la use cuando su contrato soporte `{speakerSample}` o equivalente.
