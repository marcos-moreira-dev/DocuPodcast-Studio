# Tanda 62 — Refactor prioritario de coordinadores del cerebro

Base: T61 verde validada por el usuario.

## Cambios productivos

- Agrega `DocumentIntakeCoordinator` para importar, perfilar y adjuntar documentos fuente como Documento narrable.
- Agrega `SourceDocumentRefreshCoordinator` para ejecutar el flujo de **Refrescar contenido** sobre fuentes solo lectura.
- Agrega `WorkspaceNavigationCoordinator` para que navegar entre workspaces no ensucie el proyecto por sí solo.
- Agrega método `refreshSourceDocument()` en `DocuPodcastShellViewModel`.
- Expone **Refrescar contenido** como acción secundaria en Documento usando `ActionButtonFactory`, no botón hardcodeado.

## Contratos respetados

- Documento fuente solo lectura.
- Documento narrable como raíz del producto.
- Guion como proyección interna/avanzada.
- Storyboard/video opcional.
- Refactor del cerebro antes del rediseño visual aplicado.

## Tests agregados

- `WorkspaceNavigationCoordinatorTest`
- `BrainCoordinatorRefactorSourceTest`

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```
