# TI2 — Audio jobs desde RenderPlan

## Estado

Implementada.

## Resultado de la tanda

La generación de audio puede consumir `RenderUnitPlan` mediante `AudioGenerationRequest`. El contrato conserva compatibilidad con jobs legacy por segmento, pero la ruta nueva genera por `AudioGenerationUnit`.

## Implementado

1. `AudioGenerationUnit` como unidad efectiva de TTS.
2. `AudioGenerationRequest` con `RenderUnitPlan` opcional.
3. `generationUnits()` filtra solo unidades que requieren voz sintetizada.
4. `MockAudioGenerationGateway` usa `request.generationUnits()`.
5. `LocalTtsProcessAudioGenerationGateway` usa `request.generationUnits()` y respeta `voiceProfileId` efectivo por unidad.
6. `AudioWorkflowCoordinator` expone request con RenderUnitPlan.
7. `DocuPodcastShellViewModel` construye `NarrationRenderPlan → RenderUnitPlan → AudioGenerationRequest` antes de enviar/reanudar audio.

## No genera audio para

- audio externo ya asignado;
- visuales silenciosos;
- unidades omitidas.

## Pendiente para tandas posteriores

- Renombrar o evolucionar `AudioSegmentSnapshot` hacia `AudioUnitSnapshot` si se decide subir formato.
- Playback por unidad.
- Exportación final WAV desde unidades mixtas.
