# Tanda 81A — Navegación limpia y superficies oficiales

## Resumen

T81A inicia el rediseño frontal de DocuPodcast Studio después de congelar el cerebro V1. La tanda reorganiza la barra de menú para que se parezca a una aplicación de escritorio común y no a una lista de módulos técnicos. También convierte la pantalla de inicio en una presentación operativa del producto.

## Cambios implementados

- Barra de menú reorganizada: `Archivo`, `Editar`, `Ver`, `Documento`, `Lectura`, `Herramientas`, `Exportar`, `Configuración`, `Ayuda`.
- Eliminados de primer nivel: `Narración`, `Voz`, `Storyboard`, `Audio`, `Reproducción`.
- `Documento` se define como menú del documento fuente, no del proyecto.
- `Archivo` incluye `Abrir carpeta del proyecto`.
- `Ver` incluye `Pantalla completa`.
- Exportación normal queda en paquete, audio final y video simple.
- Se retiran de la navegación normal `Importar narración Markdown` y `Exportar reporte diagnóstico`.
- `WelcomeWorkspaceView` usa acciones reales y componentes transversales estilizados.
- Se agrega documentación detallada en `docs/productizacion/FRONTEND_NAVIGATION_SURFACES_T81A.md`.

## Archivos principales modificados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java`
- `src/main/resources/css/welcome.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/MainMenuNavigationSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeProductLandingSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ux/VisibleActionContractSourceTest.java`

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Focos:

- `MainMenuNavigationSourceTest`
- `WelcomeProductLandingSourceTest`
- `VisibleActionContractSourceTest`
- regresión de tests de componentes GUI y shell

## Próximo paso

T81B — Documento limpio: retirar metadatos técnicos del workspace central y preservar la concentración de lectura.
