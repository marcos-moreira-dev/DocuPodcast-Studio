# Tanda 73 — Audio jobs robustos

## Propósito

Cerrar otra pieza del cerebro V1: los trabajos de audio no deben ser solo una cola visible. Deben poder auditarse, reanudarse, reutilizar WAV existentes y detectar cuándo el audio queda obsoleto por cambios en el documento fuente.

## Alcance implementado

- Se corrige el guardarraíl local heredado de T72: `RefreshSourceDocumentUseCase` declara explícitamente `UNSUPPORTED` para PDFs/fuentes que no cumplen contrato.
- Se agrega `AudioJobHealthStatus`.
- Se agrega `AudioJobMaintenanceReport`.
- Se agrega `InspectAudioJobMaintenanceUseCase`.
- `AudioApplicationServices` expone `inspectAudioJobMaintenance`.
- `AudioWorkflowCoordinator` puede pedir un `maintenanceReport` del job activo.

## Reglas de producto

1. Un job completo con WAVs presentes puede reutilizar audio existente.
2. Un job con segmentos pendientes, fallidos o cancelados debe ser reanudable.
3. Un job con WAVs faltantes debe marcarse como `MISSING_AUDIO`.
4. Si `Refrescar contenido` detecta cambios de texto, el audio queda obsoleto (`STALE_SOURCE`).
5. El usuario no debe perder WAVs existentes por defecto; la regeneración debe ser decisión explícita o política controlada.

## Criterio de salida

Los tests deben pasar y el cerebro debe poder responder: listo, reanudable, obsoleto, faltan audios o requiere revisión.
