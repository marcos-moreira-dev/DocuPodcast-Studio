# Tanda 33 — Export bundle auditable

## Objetivo

Convertir la exportación de paquete completo en una salida auditable y verificable para entrega de proyecto, no solo una copia de carpetas.

## Cambios principales

- Se agregó `BundleArtifactMetadata` para representar archivos incluidos con ruta relativa, tamaño y SHA-256.
- `ProjectBundleExportResult` ahora expone `fileIndexFile`, `copiedAssets` y la lista de artefactos auditados.
- `FileSystemProjectBundleExporter` genera un paquete con:
  - `input/`
  - `editable/`
  - `output/`
  - `assets/`
  - `jobs/`
  - `reports/`
  - `README_EXPORTACION.md`
  - `MANIFEST_EXPORTACION.md`
  - `reports/BUNDLE_FILE_INDEX.tsv`
- Los assets registrados se copian explícitamente bajo `assets/registered/<kind>/...`.
- El manifiesto incluye conteos, resumen de guion/storyboard/jobs y tabla SHA-256.
- El índice TSV permite auditoría externa simple del paquete.

## Decisión de producto

El paquete completo debe servir para entrega, auditoría y soporte. Por eso debe contener entradas originales, editables, outputs, jobs, assets registrados, manifiesto e índice con hashes.

## Validación recomendada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

Luego probar manualmente la acción `Exportar paquete completo` con un proyecto que tenga documento, guion, storyboard, audio/jobs y assets.
