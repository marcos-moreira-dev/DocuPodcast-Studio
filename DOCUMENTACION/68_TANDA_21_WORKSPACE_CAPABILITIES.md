# Tanda 21 — Workspace registry, descriptors y capabilities

## Objetivo

Convertir los workspaces y la toolbar contextual en una superficie de producto basada en descriptores y capacidades explicitas, no en switches dispersos dentro del shell.

## Cambios principales

- Se agrego `WorkspaceCapability` para nombrar capacidades visibles del producto.
- Se agrego `WorkspaceDescriptor` y `WorkspaceDescriptorCatalog` como catalogo oficial de workspaces.
- Se agrego `WorkspaceRouteResolver` para resolver rutas persistidas o nulas hacia un workspace seguro.
- Se agrego `WorkspaceViewRegistry` para registrar factories de vistas y usar `PlaceholderWorkspaceView` solo como fallback controlado.
- Se agrego `WorkspaceCapabilityPolicy` para deshabilitar acciones segun estado real: documento cargado, guion existente, proyecto abierto, seleccion activa o job de audio.
- Se agrego `WorkspaceToolbarActionProvider` y `WorkspaceToolbarAction` para que la toolbar contextual salga de un proveedor de acciones por workspace.
- `DocuPodcastShellView` deja de manejar un `EnumMap<WorkspaceKind, Node>` manual y usa registry + route resolver.
- `MainToolbarView` deja de decidir acciones por switch de workspace y renderiza acciones declaradas por el provider.

## Contrato de producto

Una accion visible de workspace debe tener:

```text
WorkspaceCapability
→ WorkspaceDescriptor/WorkspaceToolbarActionProvider
→ WorkspaceCapabilityPolicy
→ handler real o preparatorio documentado
→ test fuente o unitario
```

Las acciones incompletas siguen siendo preparatorias y no se presentan como ejecucion final.

## Tests agregados/actualizados

```text
WorkspaceDescriptorCatalogTest
WorkspaceRegistrySourceTest
WorkspaceToolbarActionProviderTest
ToolbarContextualWorkspaceSourceTest
ToolbarRecordingPlaybackSourceTest
VisibleActionContractSourceTest
```

## Alcance

Esta tanda no implementa nuevos flujos funcionales; prepara la estructura para que Tanda 22 y posteriores puedan separar dialogos, notificaciones, recursos IA y workspaces sin seguir aumentando deuda en el shell.
