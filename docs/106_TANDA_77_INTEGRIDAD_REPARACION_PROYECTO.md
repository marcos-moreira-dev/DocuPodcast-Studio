# Tanda 77 — Integridad y reparación del proyecto

## Objetivo

T77 agrega el primer reporte no binario de integridad del cerebro. La apertura/diagnóstico de un proyecto ya no debe limitarse a `válido` o `inválido`: ahora puede distinguir `OK / CON_ADVERTENCIAS / REQUIERE_REPARACION` para fuente, documento, capas, assets, checksums, jobs, storyboard y paquete de video simple cuando exista.

Esta tanda no rediseña la interfaz ni convierte Documento en una cabina técnica. El reporte vive en `application.project` y queda listo para que una pantalla futura lo muestre de forma humana o lo mande a Diagnóstico/Configuración.

## Cambios principales

- Nuevo contrato `ProjectIntegrityStatus` con estados `OK`, `CON_ADVERTENCIAS` y `REQUIERE_REPARACION`.
- Nuevo contrato `ProjectIntegritySeverity` para separar hallazgos informativos, advertencias y errores bloqueantes.
- Nuevo `ProjectIntegrityIssue` con código, severidad, target, mensaje y sugerencia de reparación.
- Nuevo `ProjectIntegrityReport` con resumen, contadores, mensajes y decisión de reparación.
- Nuevo `InspectProjectIntegrityUseCase` para inspeccionar:
  - carpeta del proyecto;
  - assets físicos y rutas relativas;
  - checksums SHA-256 cuando estén registrados;
  - documento/guion/storyboard materializados;
  - capas narrativas contra segmentos, bloques, voces, estilos, imágenes y audio reales;
  - bindings de storyboard contra segmentos e imágenes reales;
  - jobs de audio usando `InspectAudioJobMaintenanceUseCase`;
  - paquete de video simple si aparecen `VIDEO_SIMPLE_PLAN.md`, `RENDER_MANIFEST.json` o `render-commands.txt`.
- `ProjectApplicationServices` expone `inspectProjectIntegrity`.
- `ApplicationServicesFactory` cablea `InspectProjectIntegrityUseCase` con `audioJobRepository`.

## Decisiones

- `ValidateProjectWorkspaceIntegrityUseCase` se conserva como validador estricto legacy/binario para no romper flujos existentes.
- `InspectProjectIntegrityUseCase` es el contrato nuevo para diagnóstico recuperable y reparación gradual.
- Un asset faltante, una ruta fuera del proyecto, un checksum distinto, una capa apuntando a un target inexistente o un audio completado sin WAV físico produce `REQUIERE_REPARACION`.
- Un job reanudable o un paquete de video incompleto produce `CON_ADVERTENCIAS` cuando no bloquea todo el proyecto.
- La UI principal no recibe vocabulario técnico nuevo; esos mensajes deberán vivir luego en diagnóstico o configuración.

## Tests agregados

- `ProjectIntegrityReportTest`
- `InspectProjectIntegrityUseCaseTest`
- `ProjectIntegrityRepairSourceTest`

## Validación esperada

En entorno local con Maven/Toolchain:

```bat
scripts\02-ejecutar-tests.bat
```

Resultado esperado: build verde con el reporte de integridad incorporado al cerebro.

## Continuidad

La próxima tanda recomendada es T78 — Exportaciones del cerebro. Debe usar o respetar el nuevo reporte de integridad para no exportar paquetes finales con assets, audio, storyboard o checksums inconsistentes sin avisar.
