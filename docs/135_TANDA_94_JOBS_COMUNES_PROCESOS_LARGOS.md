# Tanda 94 — Jobs comunes para procesos largos

Tanda de arquitectura transversal previa a la repotenciación GUI. Agrega contrato común para procesos largos sin reemplazar el sistema maduro de audio TTS.

## Implementado

- `ProcessJobSnapshot`
- `ProcessJobKind`
- `ProcessJobState`
- `ProcessJobStage`
- `ProcessJobArtifact`
- `ProcessJobLogReference`
- `AudioJobProcessMapper`
- `ListProcessJobsUseCase`
- `InspectProcessJobContractUseCase`
- `ProcessApplicationServices`

## No implementado todavía

- FFmpeg como job persistente.
- Setup de motores como job persistente.
- Render MP4 real como job persistente.
- GUI de progreso común.

## Validación esperada

- Tests de dominio de process jobs.
- Tests de mapper audio → process.
- Tests de listado de jobs a través de contrato común.
- Guardarraíl fuente T94.
