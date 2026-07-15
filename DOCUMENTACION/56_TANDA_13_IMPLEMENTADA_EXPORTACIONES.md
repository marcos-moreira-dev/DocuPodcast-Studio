# Tanda 13 — Exportaciones — IMPLEMENTADA

Esta tanda agrega una primera capa real de exportación a DocuPodcast Studio.

## Exportaciones implementadas

- Guion narrable a Markdown (`docupodcast-script-v1`).
- Podcast WAV desde el último job con audio final disponible.
- Reporte diagnóstico Markdown.
- Paquete completo de proyecto en carpeta portable.

## Principio de diseño

DocuPodcast exporta según la naturaleza del artefacto:

```text
Guion      → Markdown
Audio      → WAV
Proyecto   → paquete con input/editable/output/jobs/reports
Diagnóstico→ Markdown
```

No se exporta el guion como screenshot. No se promete video de storyboard todavía. No se exporta MP3 hasta que haya encoder/configuración real.

## Paquete completo

La exportación de paquete crea:

```text
<proyecto>_export_<timestamp>/
├── input/
├── editable/
├── output/
├── assets/
├── jobs/
└── reports/
```

`input/` conserva el Word/DOCX fuente cuando existe. `editable/` conserva `.docupodcast.json` y snapshots editables. `output/` contiene guion Markdown, podcast WAV si existe y resumen de storyboard. `jobs/` copia los jobs persistidos. `reports/` incluye README y manifiesto.

## Clases principales

```text
application/export/
  ExportTargetPathPolicy
  ExportNarrationScriptMarkdownUseCase
  ExportPodcastWavUseCase
  ExportDiagnosticReportUseCase
  ExportProjectBundleUseCase
  ProjectExportFormatPolicy

infrastructure/export/
  FileSystemProjectBundleExporter
  ExportFolderNamePolicy
```

## UI

El menú `Exportar` incluye:

- Exportar guion Markdown.
- Exportar podcast WAV.
- Exportar reporte diagnóstico.
- Exportar paquete completo.

La toolbar contextual ahora llama a `handleExportProjectBundle()` para el paquete.

## Tests agregados

- `ExportTargetPathPolicyTest`.
- `ExportNarrationScriptMarkdownUseCaseTest`.
- `ProjectExportFormatPolicyTest`.
- `FileSystemProjectBundleExporterTest`.
- `ExportUiSourceTest`.

## Validación local

Ejecutar:

```bat
scripts\00-verificar-entorno.bat
scripts\03-verificar-toolchain.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

En este entorno sin Maven se validó compilación parcial con `javac --release 21` para domain/application/infrastructure/factories y smoke manual de exportación.
