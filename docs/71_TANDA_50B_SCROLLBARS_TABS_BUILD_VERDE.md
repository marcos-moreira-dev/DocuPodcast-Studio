# Tanda 50B — Scrollbars, tabs y build verde visual

## Objetivo

Corregir los guardarraíles rojos de Tanda 50 y cerrar detalles visuales básicos antes de continuar con nuevas funcionalidades.

Esta tanda responde a tres observaciones de producto:

1. Los scrollbars deben tener estilo propio y no verse como controles crudos.
2. Los tab panes deben verse integrados al tema moderno.
3. Los textos del menú no deben fusionarse con el fondo ni perder contraste.

## Cambios

- `document-reader.css` se divide para respetar el presupuesto humano de CSS modular.
- Se crea `css/document/document-page.css` para reglas modernas de página, bloques, frases y seguimiento activo.
- Se crea `css/components/chrome-controls.css` para scrollbars, tabs, context menus y menú emergente.
- `docupodcast-light.css` importa los nuevos módulos como hoja ensambladora.
- `shell.css` diferencia etiquetas del menú superior sobre fondo oscuro y etiquetas de menús emergentes sobre fondo claro.
- Se ajusta `VisualFeedbackCssSourceTest` para validar tokens visuales modernos en vez de exigir sombras duplicadas en cada módulo.
- Se agrega `DesktopChromeControlsSourceTest` para proteger scrollbars, tabs y menús.

## Contrato visual

La app debe verse moderna y formal, sin exagerar bordes redondeados ni apilar paneles decorativos. La pantalla principal sigue orientada a usuario no técnico; los controles de infraestructura deben vivir en Configuración o vistas avanzadas.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Debe pasar sin fallos antes de continuar con Tanda 51.
