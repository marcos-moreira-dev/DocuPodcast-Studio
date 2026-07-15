# Tanda 61 — Auditoría ejecutable del cerebro y refresco de fuente

## Resumen

Tanda 61 empieza a convertir el contrato de `Refrescar contenido` en código de cerebro, no en UI. Agrega snapshots, reporte de cambio y caso de uso de refresco para comparar el documento fuente externo contra el Documento narrable ya importado.

## Cambios productivos de bajo riesgo

Nuevos contratos de dominio:

```text
SourceDocumentSnapshot
SourceDocumentChangeStatus
SourceDocumentChangeReport
DerivedArtifactFreshness
```

Nuevos contratos de aplicación:

```text
RefreshSourceDocumentUseCase
RefreshSourceDocumentResult
```

## Qué no hace

- No agrega todavía botón visible.
- No cambia la experiencia de Documento.
- No borra audio automáticamente.
- No reconcilia rangos de capas de forma automática.
- No convierte DocuPodcast en editor de Word/PDF/Markdown/TXT.

## Validación esperada

```bat
scripts\02-ejecutar-tests.bat
```

Tests nuevos:

```text
SourceDocumentSnapshotTest
RefreshSourceDocumentUseCaseTest
BrainRefreshExecutableSourceTest
ShellViewModelBrainDebtSourceTest
```
