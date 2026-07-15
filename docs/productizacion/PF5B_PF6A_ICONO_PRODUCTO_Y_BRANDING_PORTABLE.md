# PF5B + PF6A — Icono de producto y branding portable

Esta tanda aplica la imagen generada del producto como icono real de la aplicación.

## Alcance

- `DocuPodcastStudioApp` carga iconos PNG desde `src/main/resources/branding/`.
- `scripts/14-app-image-completa.bat` y `scripts/15-msi-completo.bat` usan `--icon packaging\windows\docupodcast-icon.ico`.
- `scripts/32-preparar-app-portable-layout.bat` copia `packaging\windows` a `branding/` dentro de la carpeta portable.
- `scripts/34-smoke-app-portable-runtime.bat` verifica que el branding quedó presente tanto en app-image como en portable.

## Activos

- `packaging/windows/docupodcast-icon.ico`
- `packaging/windows/docupodcast-icon.png`
- `src/main/resources/branding/docupodcast-icon-32.png`
- `src/main/resources/branding/docupodcast-icon-64.png`
- `src/main/resources/branding/docupodcast-icon-256.png`

## Criterio de producto

El icono debe ser consistente entre la ventana JavaFX, el ejecutable generado por `jpackage`, el MSI y la carpeta portable.
