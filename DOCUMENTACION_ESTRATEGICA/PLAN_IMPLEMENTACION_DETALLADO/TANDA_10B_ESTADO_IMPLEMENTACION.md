# Tanda 10B — Estado de implementación

## Implementado

- `ImportVoiceSampleUseCase`.
- `VoiceSampleRepository`.
- `LocalVoiceSampleFileRepository`.
- Registro de `VOICE_SAMPLE` en assets.
- Enlace `VoiceProfile.sampleAssetId`.
- Acciones UI en menú, toolbar y workspace Voces.
- Tests de aplicación, infraestructura y UI fuente.

## Pendiente

- grabación desde micrófono;
- selección de destino distinto a `VOC-OWN-PLACEHOLDER` desde diálogo;
- validación de duración y sample rate;
- conversión de audio;
- integración directa con `speaker_wav` del worker TTS;
- STT/Whisper real.

## Siguiente tanda

Tanda 11 — Storyboard básico.
