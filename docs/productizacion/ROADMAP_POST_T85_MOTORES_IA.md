# Roadmap post T85 — Motores IA reales

## Estado tras T85

T85 deja listo el primer puente real para Piper:

```text
engineMode=piper → wrapper PowerShell → piper.exe → WAV por segmento
```

Esto permite una demo borrador real si el usuario instala Piper y una voz ONNX.

## Tandas restantes

### T86 — Coqui XTTS wrapper real

Objetivo: motor de voz de calidad alta.

Debe entregar:

- wrapper externo XTTS/Coqui;
- contrato de entrada texto + voz/referencia;
- salida WAV por segmento;
- compatibilidad con GPU/CPU;
- preflight de modelo y dependencias;
- documentación de licencia y uso permitido.

### T87 — whisper.cpp real

Objetivo: audio → texto.

Debe entregar:

- validación de `whisper-cli.exe`;
- validación de modelo local;
- prueba de transcripción corta;
- salida TXT/JSON con manifest;
- UX clara en Configuración.

### T88 — FFmpeg real para media

Objetivo: video/audio.

Debe entregar:

- extracción de audio desde MP4/MOV/MKV/WEBM;
- normalización WAV cuando haga falta;
- diagnóstico de FFmpeg;
- integración con `Audio del computador`.

### T89 — UX humana de motores

Objetivo: que el usuario normal entienda qué falta.

Mensajes esperados:

- `Falta configurar motor de voz`;
- `Falta voz Piper`;
- `Coqui necesita modelo local`;
- `Generando audio...`;
- `Audio listo`;
- `Error del motor: revisar configuración`.

### T90 — Smoke real + RC

Objetivo: cerrar el valor real.

Smoke mínimo:

- DOCX → Piper → WAV real → reproducción;
- DOCX → Coqui → WAV real → reproducción;
- audio → whisper.cpp → texto;
- video → FFmpeg → audio extraído;
- export bundle con manifests.
