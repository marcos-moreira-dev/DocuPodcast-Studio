# T109 — Overlay de procesos largos

## Objetivo

Reemplazar Audio Jobs como vista visible por un overlay/panel de progreso. T109 se implementa adelantada respecto a T108 por feedback del flujo de Documento.

## Estado implementado

- `LongProcessOverlayView` se monta encima del workspace activo.
- El overlay consume `activeAudioJobStatusProperty()` y muestra progreso, ETA, segmentos y cancelación.
- `DocuPodcastShellViewModel` deja de navegar a `AUDIO_JOBS` durante generación/reanudación de audio.
- Los detalles técnicos siguen disponibles en el proyecto/logs, no en la pantalla principal.

## Procesos cubiertos en T109

```text
TTS_AUDIO / generación de audio
```

## Procesos no cubiertos aún

```text
FFmpeg media preparation
setup de motores
render de video
```

## Criterio de aceptación

- El usuario ve progreso sin salir del Documento.
- Puede cancelar si el proceso lo permite.
- Audio Jobs no vuelve como workspace operativo visible.
- El overlay no muestra logs crudos ni jerga técnica innecesaria.
