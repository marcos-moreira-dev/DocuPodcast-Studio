# Exportación DocuPodcast

Este paquete contiene entrada, proyecto editable, salidas, assets, jobs y reportes auditables.

- Proyecto: Smoke cerebro T79
- Fecha UTC: 2026-07-20T02:08:21.666832600Z

Carpetas principales:

- `input/`: Word/DOCX u otra fuente original cuando esté disponible.
- `editable/`: `.docupodcast.json` y snapshots editables/materializados.
- `output/`: lectura preparada, audio final y resúmenes exportados.
- `assets/`: copia auditable de assets registrados por tipo.
- `jobs/`: jobs persistidos para auditoría o recuperación.
- `reports/`: índice de archivos y copia del manifiesto.

Archivos raíz:

- `MANIFEST_EXPORTACION.md`: resumen del paquete y SHA-256 de artefactos.
- `reports/BUNDLE_FILE_INDEX.tsv`: índice tabular de archivos, tamaños y SHA-256.
- `reports/EXPORT_READINESS.md`: matriz de salidas exportables, faltantes y limitaciones honestas.
- `reports/VOICE_REFERENCE_SAMPLES.md`: matriz auditable de muestras de voz por tono.
