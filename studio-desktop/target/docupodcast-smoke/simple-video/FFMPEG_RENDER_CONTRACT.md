# Contrato de render FFmpeg embebido

- Contrato: `docupodcast-simple-video-render-v1`.
- Ruta preferida: `tools/ffmpeg/bin/ffmpeg.exe`.
- Ruta complementaria: `tools/ffmpeg/bin/ffprobe.exe`.
- No se exige tocar PATH ni instalar FFmpeg globalmente.
- Resolución por defecto: 2K — 2560x1440.
- Dispositivo de render solicitado: AUTO.
- Encoder solicitado: AUTO (`FFMPEG_DEFAULT`).
- Opciones de usuario: 720p, 1080p, 2K y 4K.
- Manifest generado: `RENDER_MANIFEST.json`.
- Comandos generados: `render-commands.txt`.
- Estado del paquete: PACKAGE_NEEDS_REVIEW.
- Este paquete es renderizable y auditable; la exportación no crea el MP4 final por sí sola.
- El render MP4 queda listo solo cuando audio, imágenes y FFmpeg están disponibles.
- Mientras se renderiza, la pantalla operativa entra en modo render bloqueante con barra de progreso y cancelación segura.
