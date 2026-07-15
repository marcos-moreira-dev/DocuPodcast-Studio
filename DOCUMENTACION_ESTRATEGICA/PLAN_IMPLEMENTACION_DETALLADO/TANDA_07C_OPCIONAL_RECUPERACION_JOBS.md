# Tanda 7C opcional — Recuperación operativa de jobs persistidos

## Motivo

La Tanda 7B persiste jobs, pero todavía no permite reanudar ni reintentar desde la UI. Antes de conectar el TTS real, esta tanda opcional permitiría validar la recuperación usando el mock.

## Alcance propuesto

- Panel de detalle de job persistido.
- Lista de segmentos con estado persistido.
- Acción `Reintentar pendientes/fallidos`.
- Acción `Continuar desde último completado`.
- Reaprovechar WAVs existentes.
- Actualizar `segments-status.json` y `job.json` durante la recuperación.
- Tests de no-regeneración de segmentos completados.

## Decisión recomendada

Hacerla antes de Tanda 8 si se quiere reducir riesgo al conectar XTTS/Piper. Saltarla es posible, pero el primer gateway real será más frágil.
