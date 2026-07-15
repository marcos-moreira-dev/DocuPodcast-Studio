# Tanda 22 — Diálogos y notificaciones de producto

## Objetivo

Separar la mensajería de producto del shell y dejar una base reutilizable para errores, confirmaciones y diálogos especializados.

## Cambios principales

- Se agrega `UserNotification` y `UserNotificationLevel` como contrato de notificación agnóstico de JavaFX.
- Se agrega `ExceptionAlertPresenter` para mostrar mensajes humanos y detalle técnico expandible.
- Se agrega `DialogStyler` para aplicar owner y estilo común a diálogos de producto.
- Se agregan diálogos especializados:
  - `ProjectNameDialog`
  - `ActiveAudioJobDialog`
  - `UnsavedChangesDialog`
  - `ExportAiResourcesResultDialog`
- `DocuPodcastShellView` deja de crear directamente `Alert`, `TextInputDialog` y `ButtonType` para estos flujos.
- `GuideDialog` reutiliza `DialogStyler`.
- `shell.css` define estilos básicos para `.product-dialog`.

## Guardarraíles

Se agregan tests fuente:

```text
ProductNotificationSourceTest
ProductDialogsSourceTest
```

Estos tests protegen que:

- `UserNotification` siga sin depender de JavaFX.
- Los errores puedan mostrar detalle técnico expandible.
- El shell delegue diálogos especializados.
- El shell no vuelva a construir `Alert`, `TextInputDialog` o `ButtonType` directamente para los flujos cubiertos.

## Alcance

Esta tanda no cambia el flujo funcional de proyecto, documento, guion, audio o storyboard. Prepara la superficie de producto para errores más claros, diálogos consistentes y futura separación de otros flujos del shell.
