# Tanda 58 — Exportación de video simple 2K con FFmpeg embebido y modo render

## Propósito

Consolidar el contrato de video simple como capacidad del producto cuando el usuario usa storyboard, imágenes o frames asociados. La salida predeterminada es 2K, con opciones configurables 720p, 1080p, 2K y 4K.

## Cambios principales

- Se agrega `SimpleVideoResolutionPreset` con presets 720p, 1080p, 2K y 4K.
- Se agrega `SimpleVideoExportSettings` con 2K como valor predeterminado.
- Se agrega `EmbeddedFfmpegLocator` y `FfmpegToolDiscovery` para preferir `tools/ffmpeg/bin/ffmpeg.exe` sin exigir PATH global.
- Se agrega `VideoRenderProgress`, `VideoRenderStage` y `VideoRenderProgressView` para modelar modo render bloqueante.
- La exportación de paquete de video simple ahora genera `FFMPEG_RENDER_CONTRACT.md` y un `render-video-simple.bat` orientado a FFmpeg embebido o configurado.
- Configuración → Storyboard / video documenta resolución, FFmpeg embebido y bloqueo de lectura/edición durante render.

## Contrato de producto

Durante el render de video, la pantalla operativa debe entrar en modo render: mostrar progreso, etapa actual y opción de cancelar si es seguro. Deben bloquearse nuevas exportaciones, cambios en asignaciones y reproducción/lectura normal que compita por recursos.

## Pendiente

La conexión completa a FFmpeg con ejecución de comandos por frame y generación directa de MP4 final queda preparada por contrato, pero debe terminarse con smoke real y manejo de errores de FFmpeg.
