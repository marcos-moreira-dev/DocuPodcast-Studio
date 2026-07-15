# Tanda 62B — Hotfix build verde de coordinadores

T62 introdujo `DocumentIntakeCoordinator`, `SourceDocumentRefreshCoordinator` y `WorkspaceNavigationCoordinator`, pero el build local falló por un source test documental: `BrainCoordinatorRefactorSourceTest` esperaba la frase `does not mark` dentro de `WorkspaceNavigationCoordinator`.

## Corrección

Se ajusta la documentación fuente del coordinador de navegación para declarar explícitamente que:

```text
This coordinator does not mark the project as dirty when the user only changes workspace.
```

La corrección no cambia comportamiento productivo. Solo alinea el contrato fuente con la intención ya implementada: navegar entre workspaces no debe ensuciar proyectos limpios ni limpiar proyectos sucios.

## Criterio de salida

- El test `BrainCoordinatorRefactorSourceTest` debe volver a verde.
- La navegación sigue usando `replaceProjectPreservingDirty`.
- El refactor posterior debe seguir moviendo cerebro fuera de `DocuPodcastShellViewModel` sin rediseñar primero la cara visual.
