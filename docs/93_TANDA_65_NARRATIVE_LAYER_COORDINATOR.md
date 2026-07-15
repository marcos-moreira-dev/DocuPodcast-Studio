# Tanda 65 — NarrativeLayerCoordinator

## Objetivo

Continuar el refactor del cerebro de DocuPodcast sacando del `DocuPodcastShellViewModel` la lógica de capas narrativas sobre el Documento narrable.

La raíz visible de V1 sigue siendo el **Documento narrable**. Las capas de voz, audio humano, emoción, imagen, ambiente y nota son artefactos del proyecto, no anotaciones dentro del Word/PDF/Markdown/TXT fuente.

## Cambios principales

- Agrega `NarrativeLayerCoordinator` en `presentation.shell.workflow`.
- Centraliza creación de `NarrativeLayerAssignment`.
- Centraliza detección de conflictos con `NarrativeLayerAssignmentPolicy`.
- Centraliza mapeo `DocumentTextRange` → `ScriptTextRange`.
- Centraliza remoción de capa principal.
- Centraliza proyección de capas para el rail.
- `DocuPodcastShellViewModel` delega en `narrativeLayerWorkflow` y conserva solo estado visible/mensajes.

## Decisiones protegidas

- Las capas se guardan en el proyecto y no modifican el documento fuente.
- La voz/audio humano siguen siendo capas primarias incompatibles por solapamiento.
- Imagen, emoción, ambiente y notas siguen siendo capas acumulables.
- Los targets placeholder se mantienen como deuda visible hasta que el flujo use una voz/imagen/audio real seleccionado.

## Validación

Tests nuevos/modificados:

```text
BrainNarrativeLayerCoordinatorSourceTest
DocumentExactTextSelectionSourceTest
DocumentLayerAssignmentWorkflowSourceTest
```

## Siguiente paso

T66 — round-trip funcional real de Documento narrable + capas + imagen + audio + reapertura.
