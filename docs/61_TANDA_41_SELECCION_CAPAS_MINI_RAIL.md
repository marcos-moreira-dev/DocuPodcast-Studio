# Tanda 41 — Selección de capas y mini rail plegable

## Objetivo

Preparar la experiencia de producción ligera sobre el documento narrado: seleccionar un bloque/oración del Word renderizado y asociar capas de voz, audio, emoción, imagen o ambiente sin escribir acotaciones dentro del Word original.

## Cambios principales

- Se agrega el dominio `domain.assignment` con:
  - `NarrativeLayerKind`.
  - `NarrativeLayerAssignment`.
  - `NarrativeLayerAssignmentPolicy`.
- La política protege el rango narrativo: una voz o audio principal no puede pisar otra fuente principal superpuesta sin reemplazar, dividir o desasignar.
- Se agrega el componente transversal `CollapsibleMediaRail`.
- El workspace Documento incorpora un rail derecho plegable para capas.
- Se agrega `DocumentLayerRailView` como superficie de acciones rápidas:
  - Asignar voz IA.
  - Asignar audio del computador.
  - Asignar emoción.
  - Asociar imagen / storyboard.
  - Agregar audio ambiente.
- El rail muestra si no hay texto asignado y mantiene el lenguaje de producto: las capas viven en el proyecto, no en el Word.

## Alcance deliberado

La tanda usa el bloque seleccionado como ancla operativa. La selección exacta por oración o fragmento arrastrado queda para una tanda posterior, reutilizando el mismo concepto de rango y rail.

## No cambia

- No se persisten todavía asignaciones reales de capas.
- No se implementa drag/selección exacta de texto.
- No se implementa todavía mini storyboard con miniaturas reales.
- No se altera la generación de audio, exportación WAV ni persistencia del proyecto.
