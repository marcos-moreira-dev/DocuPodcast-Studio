# Tanda 38 — Kit GUI transversal y CSS modular

## Objetivo

Evitar que la interfaz de DocuPodcast crezca como JavaFX suelto repetido en cada workspace. La pantalla principal debe seguir siendo amable para un usuario no profesional, mientras la complejidad interna queda detrás de componentes reutilizables y módulos CSS pequeños.

## Cambios

- Se agrega el paquete `presentation.components` con componentes GUI transversales:
  - `AppStyles`
  - `PrimaryActionStrip`
  - `EmptyStateView`
  - `SectionHeader`
  - `SettingsPageView`
- `DocumentWorkspaceView` reutiliza `PrimaryActionStrip` y `EmptyStateView` para la operación principal del documento narrado.
- `SettingsDialog` reutiliza `SettingsPageView` para sus páginas internas, reduciendo layout JavaFX repetido.
- `docupodcast-light.css` queda como hoja ensambladora.
- Se agregan módulos CSS de componentes:
  - `css/components/actions.css`
  - `css/components/cards.css`
  - `css/components/settings-shell.css`
- Las reglas históricas que estaban incrustadas en `docupodcast-light.css` pasan a `compat-legacy.css` como etapa de compatibilidad.

## Guardarraíles

Se agregan tests fuente para proteger esta dirección:

- `GuiComponentReuseSourceTest`
- `CssModularitySourceTest`
- `NoInlineStyleSourceTest`

## Decisión de producto

JavaFX nativo sigue permitido, pero las nuevas superficies de Documento, reproducción, mini rail, configuración y storyboard deben apoyarse primero en componentes GUI compartidos. El CSS debe crecer por módulos, no en archivos gigantes.

## Validación local esperada

```bat
scripts\02-ejecutar-tests.bat
```
