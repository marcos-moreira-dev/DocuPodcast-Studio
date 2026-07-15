# TE1 — Exportaciones alineadas a voces por tono

Estado: implementada sobre TC1 verde.

## Objetivo

Alinear el paquete exportado con el modelo vigente de Voces: una voz puede tener una muestra neutral y muestras adicionales por tono. La exportación debe auditar esas muestras sin depender de lenguaje legacy ni de nombres técnicos de motores en la interfaz normal.

## Cambios implementados

- Se agrega `BuildVoiceReferenceSamplesExportReportUseCase`.
- Se agrega `VoiceReferenceSamplesExportReport`.
- `FileSystemProjectBundleExporter` genera:
  - `reports/VOICE_REFERENCE_SAMPLES.md`
  - `reports/VOICE_REFERENCE_SAMPLES.tsv`
- `MANIFEST_EXPORTACION.md` incluye:
  - muestras de voz por tono;
  - muestras con asset registrado;
  - muestras con archivo faltante;
  - pruebas de voz generadas.
- `README_EXPORTACION.md` documenta el nuevo reporte.
- `ProjectBundleExportResult` expone:
  - `voiceReferenceSamplesReportFile`
  - `voiceReferenceSamplesIndexFile`
  - `voiceReferenceSampleCount`
- `ExportWorkflowCoordinator` informa el conteo de muestras de voz exportadas.

## Contrato

- Las muestras se auditan por voz, tono, asset, ruta, ownership y origen.
- La muestra neutral se documenta como referencia base.
- Los tonos faltantes pueden usar fallback neutral según el flujo Documento/Voces.
- Las pruebas generadas en `voices/generated-tests/` se cuentan como evidencia exportable.
- No se reintroducen nombres técnicos de motores en mensajes visibles de exportación.

## Tests

- `BuildVoiceReferenceSamplesExportReportUseCaseTest`
- `ExportVoiceSamplesTe1SourceTest`
- Actualización compatible de `ExportBundleAuditSourceTest`.

## Validación focal

- `javac --release 21` de dominio, aplicación e infraestructura.
- Compilación focal de `ExportWorkflowCoordinator`.
- Compilación focal de tests TE1/exportación.
- Ejecución reflexiva de tests seleccionados.
- Smoke manual de exportación de bundle con muestra neutral.
