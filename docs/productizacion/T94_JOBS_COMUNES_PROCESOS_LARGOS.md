# T94 — Jobs comunes para procesos largos

## Objetivo

T94 introduce un contrato común para procesos largos locales sin rehacer todavía todos los motores. El audio TTS ya tenía un sistema maduro de jobs; esta tanda lo expone mediante una fachada transversal para que los próximos procesos puedan converger con el mismo lenguaje de estado, progreso, cancelación, logs y artefactos.

## Alcance implementado

- Nuevo paquete `domain.process`.
- Nuevo paquete `application.process`.
- `ProcessJobSnapshot` como snapshot común.
- `ProcessJobKind` con familias de producto:
  - `TTS_AUDIO`
  - `MEDIA_PREPARATION`
  - `ENGINE_SETUP`
  - `VIDEO_RENDER`
- `ProcessJobState` y `ProcessJobStage` como ciclo de vida común.
- `ProcessJobArtifact` y `ProcessJobLogReference` para artefactos/logs project-relative.
- `AudioJobProcessMapper` para mapear jobs TTS existentes al contrato común.
- `ListProcessJobsUseCase` para listar jobs persistidos mediante el contrato común.
- `InspectProcessJobContractUseCase` para saber qué procesos ya están respaldados por jobs persistentes y cuáles quedan pendientes.
- `ProcessApplicationServices` expuesto en `ApplicationServices`.

## Decisión conservadora

T94 no reemplaza `AudioJobSnapshot`, `AudioGenerationGateway` ni los repositorios existentes de audio. El job de TTS ya funciona y conserva su contrato especializado. Esta tanda agrega una capa común para que GUI, diagnóstico y futuros jobs de FFmpeg/setup/video no inventen su propio sistema.

## Qué queda pendiente

- Convertir normalización/extracción FFmpeg a jobs persistentes.
- Convertir setup de motores locales a jobs de proceso.
- Convertir render de video real a job persistente/cancelable.
- Unificar vistas de progreso/cancelación cuando se aborde la GUI.

## Criterio de producto

La interfaz futura debe poder mostrar procesos largos de forma homogénea:

```text
Generando voz
Preparando audio/video
Preparando motor de voz
Renderizando video
```

sin exponer detalles técnicos como stdout/stderr salvo en diagnóstico.
