# Roadmap posterior a T80A

1. T80B — entrada flexible de media: MP3/WAV/video→audio.
2. T80 — congelación del cerebro V1 con CPU/GPU ya modelado.
3. Lectura 15 — frontend quirúrgico antes del rediseño.
4. T81 — rediseño frontal guiado.
5. T82 — release candidate.

## Pendiente técnico deliberado

T80A no ejecuta benchmarks ni prueba encoders reales. Una tanda futura puede agregar diagnóstico opcional con `ffmpeg -encoders`, `nvidia-smi`, `wmic`, `dxdiag`, `rocm-smi` o equivalentes, siempre sin bloquear la pantalla Documento.
