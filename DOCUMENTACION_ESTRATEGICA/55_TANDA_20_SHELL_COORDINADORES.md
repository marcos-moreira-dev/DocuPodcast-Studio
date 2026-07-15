# Tanda 20 — Shell con coordinador de proyecto y ventana nativa sin barra redundante

## Objetivo

Iniciar la reducción de responsabilidad de `DocuPodcastShellViewModel` sin romper el flujo funcional ya validado en Tandas 18 y 19.

## Cambios aplicados

```text
Se agrega ProjectWorkflowCoordinator.
Se agrega OpenedProjectContext.
DocuPodcastShellViewModel delega crear, abrir, guardar y cerrar proyecto.
ProjectWorkflowCoordinator conserva abrir → validar payload → rehidratar → validar integridad → abrir sesión.
Se elimina la franja interna redundante de minimizar/maximizar/cerrar.
La aplicación conserva StageStyle.DECORATED y por tanto usa los controles nativos del sistema operativo.
Se limpian las reglas window-control-* de shell.css.
```

## Guardarraíles

```text
ProjectWorkflowCoordinatorSourceTest protege que el ViewModel no vuelva a absorber la secuencia de proyecto.
WindowControlsAndStageFitSourceTest protege que no vuelva la barra interna duplicada.
VisualFeedbackCssSourceTest valida statusbar/welcome y ausencia de controles de ventana internos.
```

## Validación local en este entorno

```text
javac --release 21 sobre domain + application + ProjectWorkflowCoordinator
javac --release 21 de DocuPodcastShellViewModel con stubs JavaFX mínimos
javac --release 21 de tests nuevos/modificados con stubs JUnit
revisión estática de ausencia de window-control-* en main y CSS
```

## Pendiente

La siguiente tanda es `Tanda 21 — Workspace registry, descriptors y capabilities`.
