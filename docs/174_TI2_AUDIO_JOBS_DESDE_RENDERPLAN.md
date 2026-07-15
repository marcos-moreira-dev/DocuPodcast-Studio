# 174 — TI2 Audio jobs desde RenderPlan

TI2 conecta el contrato `RenderUnitPlan` de TI1 con la cola de generación de audio. La app conserva la ruta legacy por segmentos, pero los nuevos jobs pueden generar por unidad efectiva.

## Archivos principales

- `application/audio/AudioGenerationUnit.java`
- `application/audio/AudioGenerationRequest.java`
- `infrastructure/audio/MockAudioGenerationGateway.java`
- `infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java`
- `presentation/shell/workflow/AudioWorkflowCoordinator.java`
- `presentation/shell/DocuPodcastShellViewModel.java`

## Regla

El TTS solo genera unidades habladas sin `audioAssetId`. Los clips externos y visuales silenciosos no entran a la cola TTS.

## Próximo paso

TI3 debe hacer que storyboard/video consuman `RenderUnitPlan` y no todos los segmentos narrables.
