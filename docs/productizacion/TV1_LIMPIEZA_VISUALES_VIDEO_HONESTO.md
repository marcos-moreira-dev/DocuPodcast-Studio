# TV1 — Limpieza de visuales/video honesto

Base: TE1 verde confirmada por el usuario.

## Objetivo

Alinear el lenguaje visible y los contratos de video para que DocuPodcast hable de **Visuales / Secuencia visual** en la UX normal y no prometa un MP4 final cuando la exportación solo prepara un paquete renderizable y auditable.

## Cambios implementados

- `SimpleVideoPackageExportResult` expone ahora:
  - `renderModeLabel`
  - `renderableAsMp4`
  - `outputFileName`
  - `honestStatusLabel()`
- `ExportSimpleVideoPackageUseCase` declara explícitamente que la exportación no crea el MP4 final por sí sola.
- `ExportWorkflowCoordinator` informa: **Paquete de video simple preparado**, estado honesto y salida esperada.
- `InspectExportReadinessUseCase` evita presentar el MP4 como resultado directo de la exportación.
- La UX normal reemplaza frases visibles de `storyboard` por **Visuales** o **Secuencia visual** en Documento, ayuda, componentes y superficies heredadas.
- `StoryboardDocument` conserva nombres internos por compatibilidad, pero sus títulos por defecto pasan a **Secuencia visual**.
- `ProjectKind.STORYBOARD` muestra **Secuencia visual** como nombre de producto.

## Regla vigente

- **Visuales / Secuencia visual** es lenguaje de usuario.
- `storyboard` puede mantenerse como paquete/clase/campo interno legacy.
- La exportación de video simple prepara paquete, plan, CSV, manifiesto y comandos.
- El MP4 solo existe después de ejecutar FFmpeg o un job de render explícito.

## Tests

- `VideoHonestyTv1SourceTest`
- `VisualLanguageTv1SourceTest`
- Actualizaciones en tests de video/storyboard/documentación para el contrato nuevo.

## Próxima tanda

TD1 — Cuarentena documental histórica.
