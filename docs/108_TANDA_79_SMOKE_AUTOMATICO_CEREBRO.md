# Tanda 79 — Smoke automático del cerebro

## Objetivo

Convertir el smoke del producto en una prueba automática del núcleo, sin JavaFX y sin depender de capturas o de navegación manual. La tanda no rediseña la interfaz: valida que los contratos del cerebro puedan ejecutarse juntos de forma reproducible.

## Cambios principales

- Nuevo paquete `application.smoke` con `BrainSmokeReport`, `BrainSmokeStep` y `BrainSmokeStepStatus`.
- Nuevo test integral `BrainSmokeScenarioTest` que ejecuta el flujo de cerebro de punta a punta.
- Nuevo script `scripts/18-smoke-automatico-cerebro.bat` para correr el escenario focalizado.
- Nueva evidencia generada en `target/docupodcast-smoke/`.
- Nuevo guardarraíl `AutomaticBrainSmokeSourceTest` para impedir que el smoke vuelva a ser solo checklist manual.

## Flujo cubierto

El smoke automático valida:

1. Importar DOCX, TXT, Markdown y PDF nativo.
2. Rechazar PDF escaneado o sin texto nativo, sin OCR.
3. Construir narración interna desde Documento.
4. Asignar una imagen real como capa del proyecto.
5. Construir storyboard desde capas `IMAGE` reales.
6. Generar audio mock persistido por segmentos.
7. Construir manifest de reproducción sincronizada.
8. Guardar y reabrir proyecto con `ProjectRoundTripUseCase`.
9. Inspeccionar integridad con `InspectProjectIntegrityUseCase`.
10. Inspeccionar exportaciones con `InspectExportReadinessUseCase`.
11. Exportar paquete auditable con `FileSystemProjectBundleExporter`.
12. Exportar paquete de video simple con `ExportSimpleVideoPackageUseCase`.

## Evidencia esperada

Al ejecutar el smoke focalizado se generan, como mínimo:

```text
 target/docupodcast-smoke/SMOKE_REPORT.md
 target/docupodcast-smoke/PROJECT_INTEGRITY.md
 target/docupodcast-smoke/EXPORT_READINESS.md
 target/docupodcast-smoke/project-tree.txt
```

También pueden generarse subcarpetas de paquete auditable y paquete de video simple dentro de `target/docupodcast-smoke/`.

## Regla de producto

El smoke automático no sustituye al smoke visual/manual de UX. Su función es proteger que el cerebro pueda importar, narrar, generar audio mock, guardar/reabrir, inspeccionar integridad, decidir exportaciones y producir paquetes auditables sin que la interfaz gráfica o el usuario tengan que corregir manualmente estados internos.

## Fuera de alcance

- No se rediseña frontend.
- No se toca CPU/GPU; queda para T80A.
- No se agrega MP3/WAV/video→audio; queda para T80B.
- No se empaqueta release candidate.
