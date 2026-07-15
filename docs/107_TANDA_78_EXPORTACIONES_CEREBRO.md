# Tanda 78 — Exportaciones del cerebro

## Objetivo

Cerrar la preparación de exportaciones como contrato del cerebro, no como botones sueltos de la interfaz. La tanda agrega una matriz central que declara qué salidas son exportables, cuáles quedan bloqueadas, qué falta y qué limitaciones deben mostrarse de forma honesta.

## Cambios principales

- Nuevo `InspectExportReadinessUseCase` en `application.export`.
- Nuevos contratos `ExportReadinessReport`, `ExportReadinessItem`, `ExportReadinessStatus` y `ExportableArtifactKind`.
- `ExportApplicationServices` expone `inspectExportReadiness` para que UI, smoke y diagnósticos puedan consultar el mismo criterio.
- `FileSystemProjectBundleExporter` escribe `reports/EXPORT_READINESS.md` dentro del paquete auditable.
- `ProjectBundleExportResult` expone `exportReadinessFile`.
- El manifiesto del paquete incluye estado de exportaciones, salidas exportables y salidas bloqueadas.

## Salidas evaluadas

- Paquete completo auditable.
- Reporte diagnóstico.
- Guion narrable Markdown.
- Podcast WAV.
- Storyboard / capas de imagen.
- Paquete de video simple.

## Regla de producto

La exportación no inventa artefactos. Si falta guion, audio, storyboard, FFmpeg, imágenes o proyecto guardado, el cerebro debe reportarlo como faltante o limitación, no prometer una salida final inexistente.

## Fuera de alcance

- No se rediseña frontend.
- No se ejecuta FFmpeg.
- No se agregan CPU/GPU ni selección de dispositivo; queda para T80A.
- No se agrega MP3/WAV/video→audio; queda para T80B.
- No se convierte el storyboard en editor de video.
