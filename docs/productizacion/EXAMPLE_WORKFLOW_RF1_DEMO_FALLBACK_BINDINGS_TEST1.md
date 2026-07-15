# EXAMPLE-WORKFLOW-RF1 + DEMO-FALLBACK-NOTICES-HF1 + DEMO-ASSET-BINDINGS-TEST1

## Objetivo

Cerrar el flujo de ejemplos demo sin dejar la orquestación dentro del `DocuPodcastShellView` y asegurar que el demo teatral comunique cualquier asociación visual parcial de forma visible para el usuario.

## Cambios

- Se agrega `ExampleProjectCreationWorkflow` como workflow de presentación para crear el proyecto demo.
- `DocuPodcastShellView` conserva únicamente la selección de ejemplo y destino; delega la secuencia operativa.
- `ExampleVisualBindingWorkflow.Result` expone `hasFallbacks()` y `userDecision(...)`.
- Si alguna imagen demo no se puede asociar automáticamente, se emite `UserVisibleDecision.defensiveFallback(...)` y el shell puede mostrar message box.
- Se agregan tests fuente para proteger los 24 assets/bindings del demo teatral.

## Criterio operativo

El demo teatral debe abrirse como proyecto preparado. Si alguna imagen queda pendiente, la aplicación debe decirlo explícitamente y orientar al usuario a completarla desde Documento > Imagen. No se agregan tarjetas decorativas ni regiones sin acción.

## Validación local ChatGPT

- Source tests nuevos compilados con stubs JUnit.
- Source tests nuevos ejecutados por reflexión.
- `ExampleVisualBindingWorkflow` compiló focalmente.
- `DocuPodcastShellViewModel` permanece bajo el límite transitorio de 2700 líneas.
