# Estado de render de video simple

- Contrato: `docupodcast-simple-video-render-v1`
- Estado: PACKAGE_NEEDS_REVIEW
- Salida esperada: `video-simple.mp4`
- Política dispositivo: AUTO
- Encoder: AUTO / FFMPEG_DEFAULT
- Hardware acceleration ready: false
- Fallback: FFmpeg no disponible; paquete auditable hasta configurar herramienta.
- Bloqueo operativo: lectura normal y nuevas exportaciones quedan bloqueadas durante el render.
- Cancelación segura: detener FFmpeg conserva el paquete para reintentar.

## Advertencias

- Hay frames sin imagen; el video renderizable solo debe incluir unidades visuales asignadas.
- FFmpeg no está disponible; el paquete queda auditable y renderizable cuando se configure tools/ffmpeg o una ruta externa.
