# Tanda 41 — Selección de capas y mini rail plegable

Esta tanda inicia la capa operativa de producción sobre el documento narrado. El documento sigue siendo la pantalla principal; las voces, audios, emociones e imágenes se gestionan como capas del proyecto DocuPodcast.

La implementación agrega un rail lateral plegable de capas, inspirado en un mini storyboard o panel compacto de editor multimedia. El rail no debe dominar la pantalla: se puede ocultar y volver a mostrar.

La protección de asignaciones se formaliza en dominio mediante `NarrativeLayerAssignmentPolicy`: los rangos pueden acumular emoción, imagen, audio ambiente o notas, pero no dos fuentes principales de narración incompatibles sobre el mismo rango.
