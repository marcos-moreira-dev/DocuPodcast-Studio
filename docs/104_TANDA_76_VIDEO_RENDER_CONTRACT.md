# Tanda 76 — Video package / render contract

## Objetivo

Cerrar el cerebro de video simple sin fingir que el frontend de render ya está terminado. La tanda convierte la exportación de video en un paquete auditable con contrato de render explícito, comandos FFmpeg, manifest, estado y reglas de bloqueo/cancelación.

## Decisión de producto

DocuPodcast sigue siendo un lector narrado. El video es una salida derivada del Documento narrable:

```text
Documento fuente solo lectura
→ Documento narrable
→ capas IMAGE reales
→ narración/audio por fragmento
→ plan de video simple
→ paquete renderizable / MP4 cuando FFmpeg y assets estén listos
```

La duración de cada frame se calcula desde el audio del segmento más el silencio configurado. No se introduce una línea de tiempo manual tipo editor de video.

## Archivos nuevos del paquete

Además de `VIDEO_SIMPLE_PLAN.md`, `frames.csv`, `ffmpeg-concat.txt`, `render-video-simple.bat` y `FFMPEG_RENDER_CONTRACT.md`, T76 agrega:

- `RENDER_MANIFEST.json`: estado de render, resolución, cantidad de frames, duración, warnings y modo.
- `render-commands.txt`: comandos FFmpeg auditables por frame y concat final.
- `RENDER_STATE.md`: resumen humano de si el paquete está listo, requiere FFmpeg o necesita revisión.

## Estados

- `MP4_RENDER_READY`: audio, imágenes y FFmpeg listos.
- `PACKAGE_READY_FFMPEG_REQUIRED`: paquete correcto, falta FFmpeg.
- `PACKAGE_NEEDS_REVIEW`: faltan audio, imagen o frames.

## No resuelto todavía

T76 no intenta resolver toda la UX frontal del render. Queda para tandas posteriores la integración visual completa: overlay de progreso real, cancelación desde UI y ejecución controlada de FFmpeg desde la aplicación.

## Validación

- `BuildVideoRenderCommandPlanUseCaseTest`
- `ExportSimpleVideoPackageUseCaseTest`
- `VideoRenderContractSourceTest`
- `SimpleVideoExportSourceTest`
