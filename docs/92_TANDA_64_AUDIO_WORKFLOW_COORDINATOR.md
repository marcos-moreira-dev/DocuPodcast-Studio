# Tanda 64 — AudioWorkflowCoordinator

## Objetivo

Continuar el refactor del cerebro de DocuPodcast separando las reglas de audio/jobs/reanudación/diagnóstico del `DocuPodcastShellViewModel`, sin rediseño visual masivo.

## Cambios principales

Se agrega:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java
```

El coordinador concentra:

- descriptor del motor de audio;
- creación de `AudioGenerationRequest`;
- envío de generación de audio;
- reanudación de jobs persistidos;
- cancelación cooperativa;
- lectura de jobs persistidos;
- selección de job activo/reanudable;
- detalle de segmentos del job;
- lectura de diagnósticos de proceso.

## Decisión de producto

El usuario sigue trabajando desde Documento narrable. Audio, jobs, manifest y diagnósticos quedan detrás como cerebro operativo o vista avanzada, no como cabina obligatoria.

## Relación con T63B

La tanda también incluye el hotfix T63B: los source tests de playback ahora aceptan que `PlaybackWorkflowCoordinator` contenga el mensaje canónico de espera de buffer.

## No incluye

- Rediseño visual de Documento.
- Configuración operativa completa de TTS/STT.
- Round-trip integral de usuario.
- Render MP4 final real.
