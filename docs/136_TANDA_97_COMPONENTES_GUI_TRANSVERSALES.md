# Tanda 97 — Componentes GUI transversales

## Resumen

T97 congela el inventario de componentes GUI compartidos antes del rediseño fuerte de la interfaz.

## Cambios

- Agrega `GuiComponentCatalog`, `GuiComponentContract`, `GuiComponentSurface` y `GuiComponentStatus`.
- Agrega componentes preparados para GUI futura: `RibbonButton`, `RibbonGroup`, `SidebarIconTab` y `RailToggleButton`.
- Agrega clases CSS y constantes `AppStyles` para los nuevos componentes.
- Agrega tests de catálogo y guardarraíles fuente.
- Documenta reglas para no hardcodear botones ni construir sobre placeholders/alucinaciones.

## Validación

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Focos nuevos:

- `GuiComponentCatalogTest`
- `GuiComponentFreezeT97SourceTest`

## Próxima tanda

T98 — Contrato GUI con el usuario antes del rediseño fuerte.
