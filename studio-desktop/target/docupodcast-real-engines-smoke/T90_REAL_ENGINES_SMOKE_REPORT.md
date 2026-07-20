# T90 — Smoke real de motores locales

- Estado: OK
- Habilitado: false
- Raíz de app: `C:\Users\MARCOS MOREIRA\Downloads\g\studio-desktop`
- Motores seleccionados/obligatorios: `coqui,piper,ffmpeg`
- Alcance: motor avanzado, voz local simple y FFmpeg. No incluye transcripcion de audio.
- Modo modular T90G: solo se ejecutan los motores listados como requeridos.

## Pasos

| Motor | Paso | Estado | Duración | Detalle |
|---|---|---|---:|---|
| opt-in | Smoke real no habilitado | Omitido | 0 ms | Ejecuta scripts\19-smoke-motores-reales.bat o usa -Ddocupodcast.realEnginesSmoke.enabled=true. |

## Evidencia esperada

- `input/` textos y fixtures generados.
- `output/` WAVs generados por motores reales.
- `logs/` stdout/stderr por proceso.
- sidecars FFmpeg `*-ffmpeg-*.log` y `*-ffmpeg-*.txt`.
