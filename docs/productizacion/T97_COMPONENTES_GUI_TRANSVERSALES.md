# T97 — Componentes GUI transversales congelados

## Propósito

T97 congela el catálogo oficial de componentes GUI antes de pisar fuerte el rediseño visual de DocuPodcast Studio.

La regla de producto queda explícita:

> No diseñar encima de alucinaciones. Primero se inventarian y congelan componentes compartidos; después se rediseñan ribbon, sidebars, workspace y rail.

## Contrato de diseño

- Si una acción aparece, debe funcionar o estar claramente deshabilitada.
- No se deben crear botones repetidos con `new Button(...)` en workspaces, sidebars ni railes.
- Las nuevas superficies deben usar componentes transversales o crear uno nuevo antes de duplicar controles.
- El workspace Documento debe mantenerse limpio para leer y escuchar.
- El sidebar izquierdo es inspector contextual del fragmento/capa.
- El rail derecho es navegación visual, no formulario de edición.
- El futuro ribbon debe usar componentes propios, no la barra estrecha heredada.

## Catálogo congelado

T97 agrega `GuiComponentCatalog` como inventario oficial. Cada entrada declara:

- componente,
- clase,
- estado,
- superficie principal,
- superficies permitidas,
- uso aprobado,
- uso prohibido.

Componentes congelados existentes:

- `ActionButtonFactory`
- `ActionBar`
- `SectionHeader`
- `InfoBadge`
- `MetricBadge`
- `DiagnosticCard`
- `EmptyStateView`
- `PrimaryActionStrip`
- `FloatingReadingControlBar`
- `TransportControls`
- `RailActionRow`
- `MediaThumbnailCard`
- `CollapsibleMediaRail`
- `SettingsPageView`
- `ToolbarActionButton` como puente heredado.

Componentes preparados para el rediseño:

- `RibbonButton`
- `RibbonGroup`
- `SidebarIconTab`
- `RailToggleButton`

## Superficies

Las superficies reconocidas son:

- `MENU_BAR`
- `LEGACY_TOOLBAR`
- `RIBBON`
- `WORKSPACE`
- `FLOATING_PLAYBAR`
- `LEFT_SIDEBAR`
- `RIGHT_MEDIA_RAIL`
- `SETTINGS`
- `DIALOG`
- `STATUS_BAR`

## Reglas específicas

### Ribbon

El futuro ribbon debe construirse con `RibbonButton` y `RibbonGroup`. No debe ser una colección de `HBox`/`Button` ad-hoc.

### Sidebar izquierdo

El inspector contextual no debe volver a mostrar etiquetas truncadas como `Det/Aud/Img`. Debe usar `SidebarIconTab` o etiquetas completas si el diseño final lo permite.

### Rail derecho

El rail de miniaturas debe usar componentes de rail y `RailToggleButton` para mostrar/ocultar. No debe sentirse incrustado en la hoja.

### Playbar

La acción primaria de escuchar documento pertenece a `FloatingReadingControlBar`, no al menú ni al ribbon como protagonista visual.

## Guardarraíles

T97 agrega pruebas para comprobar que:

- existe un catálogo oficial de componentes;
- las superficies tienen dueño;
- los futuros componentes de ribbon/sidebar/rail existen;
- CSS y `AppStyles` exponen sus clases;
- la documentación conserva la política antes del rediseño fuerte.

## Fuera de alcance

T97 no rediseña todavía:

- menu bar final,
- ribbon por pestañas,
- layout del workspace Documento,
- sidebar izquierdo definitivo,
- rail derecho retráctil final,
- guía visual final.

Eso queda para el bloque GUI posterior al contrato T98.
