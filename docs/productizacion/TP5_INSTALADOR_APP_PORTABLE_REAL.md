# TP5 — Instalador/app portable real

TP5 convierte el packaging previo en una ruta de distribución más cercana al producto: app-image, carpeta portable y MSI opcional.

## Contrato

- `scripts/14-app-image-completa.bat` genera `dist/app-image/DocuPodcastStudio` y copia `tools`, `models`, `scripts/tts` y `dist/legal` si existen.
- `scripts/32-preparar-app-portable-layout.bat` prepara `dist/portable/DocuPodcastStudio` desde el app-image y agrega recursos runtime/legal.
- `scripts/15-msi-completo.bat` conserva staging de runtime/legal antes de invocar jpackage MSI.

## Clases

- `DistributionPackageKind`
- `DistributionPackageStep`
- `DistributionPackagePlan`
- `BuildDistributionPackagePlanUseCase`

## Criterio

El usuario no debe depender de PATH global. El producto portable debe llevar o documentar `tools/`, `models/`, `scripts/tts/` y `legal/`.
