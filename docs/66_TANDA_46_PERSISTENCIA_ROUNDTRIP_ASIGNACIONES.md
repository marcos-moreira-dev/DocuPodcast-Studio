# Tanda 46 — Persistencia y round-trip de asignaciones

## Objetivo

Hacer persistentes las capas de producción que se preparan desde el Documento narrado: voz IA, audio principal, emoción/intención, imagen/storyboard, ambiente y notas.

## Cambios principales

- `DocuPodcastProject` incorpora `narrativeLayerAssignments` como parte del agregado raíz.
- `NarrativeLayerAssignment` conserva ahora opcionalmente el rango documental (`DocumentTextRange`) además del rango de guion (`ScriptTextRange`).
- `DocuPodcastProjectJsonWriter` serializa `narrativeLayers.assignments` en `.docupodcast.json`.
- `DocuPodcastProjectJsonReader` reconstruye las asignaciones y mantiene compatibilidad con proyectos previos sin `narrativeLayers`.
- La política de conflictos se mantiene: no se puede solapar una voz principal con un audio principal sobre el mismo rango sin reemplazar, dividir o desasignar.

## Contrato de producto

El Word/DOCX original sigue siendo fuente inmutable. Las voces, audios, emociones, imágenes y notas son acotaciones del proyecto DocuPodcast, no texto escrito dentro del documento original.

## Validación agregada

- `DocuPodcastProjectNarrativeLayersTest`
- `DocuPodcastProjectNarrativeLayersJsonTest`
