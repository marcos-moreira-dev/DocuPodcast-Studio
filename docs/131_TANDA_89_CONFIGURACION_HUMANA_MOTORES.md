# Tanda 89 — Configuración humana de motores

Base: T88C con corrección de tests locales.

Resumen:

- Corrige guardarraíles heredados de T88C.
- Agrega preflight visible de Coqui/XTTS, Piper y FFmpeg en Configuración.
- Mantiene Whisper/STT fuera del producto visible.
- Evita botones decorativos en la configuración de motores.
- Refuerza layout de botones inferiores para evitar truncados.

Validación esperada: suite completa Maven verde localmente con `scripts\02-ejecutar-tests.bat`.
