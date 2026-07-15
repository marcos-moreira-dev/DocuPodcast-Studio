# RF1 — Dividir `DocuPodcastShellViewModel`

RF1 inicia el refactor de presentación posterior al cierre de las tandas técnicas TI1–TI7.

La regla de esta tanda es estricta: **no cambia comportamiento visible**. La interfaz, los menús, el ribbon, la playbar, los overlays, el rail y los flujos de exportación deben seguir funcionando igual. El objetivo es reducir deuda acumulada en `DocuPodcastShellViewModel` sin rediseñar la aplicación.

## Cambio realizado

Se extrae una primera familia de flujo hacia:

```text
presentation.shell.workflow.ExportWorkflowCoordinator
```

Este coordinador concentra la orquestación de exportaciones:

- guion narrable Markdown;
- podcast WAV;
- reporte diagnóstico;
- paquete portable del proyecto;
- paquete storyboard/video desde `RenderUnitPlan` con fallback legacy.

El `DocuPodcastShellViewModel` conserva el estado UI y los mensajes visibles, pero delega el flujo operativo a `ExportWorkflowCoordinator`.

## Por qué exportación primero

Exportación era una zona de bajo riesgo para iniciar el refactor porque:

- no gobierna selección activa del documento;
- no toca directamente playback en vivo;
- no altera el render en curso;
- ya trabaja sobre use cases de aplicación estables;
- permite reducir tamaño del ViewModel sin cambiar UX.

## Decisiones

- RF1 no extrae todavía media, playback, voces ni procesos largos.
- RF1 no borra workspaces heredados.
- RF1 no cambia formato JSON.
- RF1 no cambia el contrato de `RenderUnitPlan`.
- RF1 no reintroduce STT/Whisper en superficies visibles.

## Siguientes refactors

Las tandas siguientes deben continuar con:

1. `RF2 — Coordinadores de presentación por flujo`.
2. `RF3 — Use cases de orquestación de producto`.
3. `RF4 — Limpieza de workspaces heredados`.
4. `RF5 — Limpieza final STT/Whisper de superficies activas`.

## Continuación RF1 — comodidad lectora fuera del ViewModel

Tras cerrar la serie T121-Voces, RF1 continúa con una extracción segura y sin cambio visual:

```text
presentation.shell.workflow.ReadingComfortCoordinator
```

Este coordinador concentra:

- carga de tamaño de fuente de lectura desde configuración operativa;
- límites mínimo/predeterminado/máximo del lector;
- cálculo de porcentaje de zoom;
- persistencia de `ReadingDocumentSettings.baseFontSize`;
- mensajes de error técnicos convertidos en resultado de persistencia para que el ViewModel solo actualice estado UI.

`DocuPodcastShellViewModel` conserva las propiedades JavaFX (`readingFontSize`, `statusMessage`) y los handlers públicos que usan StatusBar/Ribbon, pero deja de construir `OperationalSettings` directamente.

La tanda mantiene la regla: **no cambia comportamiento visible**. El control de zoom de lectura, el StatusBar y el lector siguen usando los mismos rangos y textos de usuario.

## Guardarraíl actualizado

- `DocuPodcastShellViewModel` debe quedar por debajo de 2500 líneas tras RF1.
- La validación/persistencia de settings de lectura no debe volver al ViewModel.
- Cualquier ampliación nueva de lectura debe pasar por `ReadingComfortCoordinator` o por un coordinador equivalente, no por lógica inline en el Shell ViewModel.
