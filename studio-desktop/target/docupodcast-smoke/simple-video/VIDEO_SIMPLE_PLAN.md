# Plan de video simple DocuPodcast

Este paquete prepara frames simples para un video de lectura narrada. Es un paquete renderizable y auditable con contrato de comandos FFmpeg. La exportación no crea el MP4 final por sí sola; el render MP4 requiere ejecutar el contrato FFmpeg o un job de render explícito.

- Contrato de render: PACKAGE_NEEDS_REVIEW
- Manifest de render: RENDER_MANIFEST.json
- Comandos auditables: render-commands.txt
- Frames: 3
- Duración estimada: 4.514 s
- Silencio posterior por frame: 1.000 s
- Resolución objetivo: 2K — 2560x1440
- Política de dispositivo: AUTO
- Encoder solicitado: AUTO
- Encoder efectivo: FFMPEG_DEFAULT
- FFmpeg embebido: preferido
- Modo render: bloquea temporalmente lectura, edición y nuevas exportaciones de video cuando se ejecute FFmpeg
- Cancelación segura: detener FFmpeg conserva el paquete y permite reintentar
- Frames sin imagen: 2
- Frames sin audio hablado requerido: 0
- Frames visuales silenciosos: 0

## Frames

### FRAME-001 — Documento Smoke T79

- Segmento: SEG-001
- Imagen: media/images/escena-001.png
- Modo: imagen-audio
- Audio: jobs/JOB-20260719-210820-001/audio/SEG-001.wav
- Duración audio: 0.396 s
- Duración frame: 1.396 s
- Texto: Nuevo tema: Documento Smoke T79.

### FRAME-002 — Primera oración narrable para validar el cerebro de DocuPodc…

- Segmento: SEG-002
- Imagen: sin-imagen
- Modo: imagen-audio
- Audio: jobs/JOB-20260719-210820-001/audio/SEG-002.wav
- Duración audio: 0.541 s
- Duración frame: 1.541 s
- Texto: Primera oración narrable para validar el cerebro de DocuPodcast.

### FRAME-003 — Segunda oración narrable para audio, playback, exportación e…

- Segmento: SEG-003
- Imagen: sin-imagen
- Modo: imagen-audio
- Audio: jobs/JOB-20260719-210820-001/audio/SEG-003.wav
- Duración audio: 0.577 s
- Duración frame: 1.577 s
- Texto: Segunda oración narrable para audio, playback, exportación e integridad.

