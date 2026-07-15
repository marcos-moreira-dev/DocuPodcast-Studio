# Tanda 47 — Exportación de video simple

## Objetivo

Preparar una salida de video simple basada en documento narrado: cada segmento/oración del guion se convierte en un frame con imagen asociada cuando existe, audio del segmento cuando está disponible y un segundo de silencio posterior.

## Cambios principales

- Se agrega `application.video` con `SimpleVideoFrame`, `SimpleVideoPlan`, `BuildSimpleVideoPlanUseCase`, `ExportSimpleVideoPackageUseCase` y `SimpleVideoPackageExportResult`.
- El plan combina guion narrable, storyboard, assets de imagen y clips WAV ya generados por segmentos.
- La exportación crea un paquete auditable con `VIDEO_SIMPLE_PLAN.md`, `frames.csv`, `ffmpeg-concat.txt` y `render-video-simple.bat`.
- La interfaz agrega la acción **Exportar video simple…** dentro del menú Exportar.
- `ExportApplicationServices` expone `exportSimpleVideoPackage()`.

## Contrato de producto

Esta tanda no incrusta todavía un encoder de video ni genera MP4 final. Prepara un paquete reproducible y auditable para conectar luego FFmpeg u otro renderizador. La pantalla principal sigue siendo Documento narrado; el video simple es una salida derivada.

## Validación agregada

- `BuildSimpleVideoPlanUseCaseTest`
- `ExportSimpleVideoPackageUseCaseTest`
- `SimpleVideoExportSourceTest`
