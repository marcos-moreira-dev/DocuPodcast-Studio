# Roadmap post T87 — media real, UX humana y RC

## Estado tras T87

- Piper tiene puente real texto → WAV.
- Coqui/XTTS tiene wrapper de calidad alta y muestra de voz por defecto.
- whisper.cpp tiene gateway STT con test end-to-end falso ejecutable.

## Próximas tandas

### T88 — FFmpeg real para media

Objetivo: cerrar `Audio del computador` y `Extraer audio de video…`.

Debe validar:

- `ffmpeg.exe` configurado o embebido;
- extracción desde MP4/MOV/MKV/WEBM;
- normalización a WAV cuando corresponda;
- logs claros;
- manifest de archivo original y audio derivado.

### T89 — UX humana de motores

Objetivo: que Documento y Configuración hablen en términos de usuario.

Mensajes esperados:

- `Falta configurar motor de voz`;
- `Falta modelo Coqui`;
- `Falta modelo Whisper`;
- `Audio generado`;
- `Transcripción lista`;
- `No se pudo extraer audio del video`.

### T90 — Smoke real + RC

Objetivo: probar el valor completo en Windows:

```text
DOCX → Coqui/Piper → WAV real → reproducción
audio → whisper.cpp → texto
video → FFmpeg → audio extraído
export bundle con manifests/logs
```
