# Roadmap post T77 — cerebro restante

## Estado de partida

T77 agrega `InspectProjectIntegrityUseCase` y el contrato `ProjectIntegrityReport`. El cerebro ya puede diferenciar `OK`, `CON_ADVERTENCIAS` y `REQUIERE_REPARACION` sin depender de JavaFX.

## Siguiente orden recomendado

### T78 — Exportaciones del cerebro

Centralizar readiness de exportación: bundle auditable, podcast WAV, diagnóstico, Markdown, storyboard/video plan, hashes y limitaciones. Debe consultar o producir información compatible con `ProjectIntegrityReport`.

### T79 — Smoke automático del cerebro

Crear smoke automático sin UI que importe documentos, prepare narración, genere audio mock, guarde/reabra, asigne capas, cree storyboard, exporte bundle/video package y valide integridad antes/después.

### T80A — Dispositivo de inferencia CPU/GPU y rendimiento

Modelar CPU/GPU/AUTO/SPECIFIC_DEVICE para TTS, STT y video. Registrar diagnóstico honesto y fallback.

### T80B — Entrada flexible de media: MP3/WAV/video a audio

Aceptar MP3/WAV como audio importable y permitir video solo para extraer audio cuando FFmpeg esté disponible. Registrar original, derivado, checksum, formato y estado.

### T80 — Congelación del cerebro V1

Cerrar capacidades, límites, contratos de no regresión y exclusiones explícitas antes del rediseño frontal.

### Lectura 15 — frontend quirúrgico

Leer el frontend con lupa antes de T81: toolbar, Documento, rail de capas, configuración, guía y CSS, respetando componentes transversales.

### T81 — Rediseño frontal guiado

Simplificar la experiencia principal: abrir documento, escuchar, seleccionar oración/rango, asignar emoción/imagen/audio de forma no invasiva y exportar.

### T82 — Release Candidate

Empaquetado, hashes, guías, licencias, FFmpeg/modelos y smoke final.
