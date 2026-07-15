# Tanda 31 — Grabación de voz humana

## Objetivo

Convertir la preparación de voz humana en una capacidad real y honesta dentro de la Biblioteca de voces: iniciar grabación local, detenerla, guardar un WAV de trabajo y registrar la muestra como asset portable del proyecto.

## Cambios principales

- Se agregó `AudioRecordingGateway` con contrato de inicio/detención con `IOException`.
- Se agregaron `StartAudioRecordingUseCase` y `StopAudioRecordingUseCase`.
- Se agregó `JavaSoundAudioRecordingGateway` para grabar WAV PCM 16 kHz mono usando Java Sound.
- `RecordingApplicationServices` ahora expone preparación, inicio y detención de grabación.
- `InfrastructureServicesFactory` inyecta el gateway de grabación.
- `DocuPodcastShellViewModel` agrega `startOwnVoiceRecording()`, `stopOwnVoiceRecording()`, `voiceRecordingRunningProperty()` y `voiceRecordingStatusLabel()`.
- `VoiceLibraryWorkspaceView` muestra botones reales para iniciar/detener grabación de Mi voz y registra la muestra al detener.
- La ayuda de voces explica que la muestra propia es referencia/capacidad según motor, no promesa de clonación.

## Contrato de producto

La grabación produce un WAV local en `recordings/` y al detenerlo se importa como muestra de voz en `voices/samples/`, actualizando `VOC-OWN-PLACEHOLDER`. La app conserva la regla ética: una muestra humana no implica síntesis/clonación real si el motor actual no la soporta.

## Tests agregados

- `JavaSoundAudioRecordingGatewaySourceTest`
- `VoiceRecordingWorkflowSourceTest`

## Validación pendiente local

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
