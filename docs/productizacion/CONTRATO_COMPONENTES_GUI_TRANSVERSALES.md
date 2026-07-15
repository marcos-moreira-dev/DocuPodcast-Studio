# Contrato de componentes GUI transversales — Tanda 59A

## Decisión de producto

DocuPodcast no debe crecer como un conjunto de pantallas que inventan controles propios. La complejidad interna puede seguir aumentando, pero la superficie visual debe expresarse mediante componentes GUI transversales con intención clara.

Regla rectora:

```text
Workspace = intención de usuario
Componente transversal = forma visual común
CSS modular = apariencia compartida
Vista concreta = composición mínima, no fábrica de botones
```

## Componentes fijados

| Componente | Uso |
|---|---|
| `ActionButtonFactory` | Crear botones primarios, secundarios, de peligro, rail, SideDock y transporte. |
| `ActionBar` | Fila horizontal de acciones de workspace/card. |
| `TransportControls` | Pausar, reanudar y detener playback sin duplicar botoneras. |
| `RailActionRow` | Fila compacta para acciones en rail/SideDock. |
| `MetricBadge` | Métricas de readiness sin repetir cards manuales. |
| `InfoBadge` | Chips/estados reutilizables. |
| `DiagnosticCard` | Cards de diagnóstico/operación avanzada. |
| `PrimaryActionStrip` | Acción principal del documento o flujo central. |
| `EmptyStateView` | Estados vacíos de producto. |
| `SectionHeader` | Encabezados de secciones. |
| `SettingsPageView` | Páginas de configuración. |
| `CollapsibleMediaRail` | Rail multimedia plegable. |

## Superficies migradas en T59A

T59A migra las botoneras repetibles de:

- `ScriptWorkspaceView`
- `AudioWorkspaceView`
- `StoryboardWorkspaceView`
- `VoiceLibraryWorkspaceView`
- `DocumentReadingProfilePanel`
- `DocumentLayerRailView`
- `DocumentActionsPanel`
- `DocumentStructurePanel`
- `WorkspaceSideDock`

## Lo permitido

Un componente transversal puede crear internamente `new Button(...)` porque su responsabilidad es encapsular el control visual.

Ejemplos permitidos:

```text
ActionButtonFactory
PrimaryActionStrip
MainToolbarView
CollapsibleMediaRail
VideoRenderProgressView
Diálogos con ButtonType
```

## Lo no permitido

Una vista de workspace no debe construir botoneras repetidas con:

```text
new Button(...)
getStyleClass().add("toolbar-button")
setOnAction(...)
```

Si la acción es recurrente, debe pasar por el catálogo compartido.

## Criterios de salida

- Las acciones principales/secundarias de workspaces usan `ActionButtonFactory` o componentes equivalentes.
- Las filas de acciones usan `ActionBar`, `TransportControls` o `RailActionRow`.
- Los resúmenes métricos nuevos deben preferir `MetricBadge`.
- La apariencia vive en `components/actions.css` y `components/cards.css`, no en estilos inline.
- Los tests de componentes fallan si vuelve `new Button(...)` a workspaces migrados.
