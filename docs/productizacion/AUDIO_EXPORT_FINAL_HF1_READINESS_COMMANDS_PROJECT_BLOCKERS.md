# AUDIO-EXPORT-FINAL-HF1 + READINESS-COMMANDS-HF1 + PROJECT-EXPORT-BLOCKERS-HF1

## Objetivo

Cerrar el flujo de exportación para que las acciones de audio/video no arranquen si faltan condiciones operativas reales.

## Cambios

- `InspectFinalAudioExportReadinessUseCase` valida el destino concreto de audio final.
- WAV se exporta con audio existente del proyecto.
- MP3/AAC requieren Video local/FFmpeg porque se comprimen desde WAV de trabajo.
- `ExportWorkflowCoordinator.exportPodcastWav(...)` revisa readiness antes de exportar.
- Los comandos de exportación requieren proyecto guardado en carpeta contenedora.
- El paquete de video simple consulta readiness antes de iniciar la exportación.

## Regla UX

Si falta audio, proyecto guardado o FFmpeg para un formato comprimido, la app debe bloquear con mensaje operativo antes de iniciar procesos largos.
