# Roadmap post T86 — motores reales

## Estado tras T86

- Piper tiene template real texto→WAV.
- Coqui XTTS tiene wrapper local y voz por defecto normalizada.
- whisper.cpp sigue pendiente de integración end-to-end real.
- FFmpeg ya existe como dependencia conceptual y debe cerrarse para extracción/normalización.

## Pendientes

1. T87 — whisper.cpp real end-to-end.
2. T88 — FFmpeg real para extracción de audio y normalización.
3. T89 — UX humana de motores, pruebas y errores.
4. T90 — Smoke real + Release Candidate real.

## Release Candidate real

No declarar RC real hasta poder demostrar:

```text
DOCX → narración → Coqui/Piper → WAV real → playback
Audio → whisper.cpp → texto
Video → FFmpeg → audio extraído
```
