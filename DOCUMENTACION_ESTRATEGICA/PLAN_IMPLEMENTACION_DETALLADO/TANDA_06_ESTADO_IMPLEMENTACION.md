# TANDA 06 — Estado de implementación

## Estado

Implementada.

## Alcance cerrado

- Dominio `domain.script`.
- Use cases de construcción, validación, edición básica de texto y materialización.
- Infraestructura `script/narration-script.json`.
- Workspace Guion básico.
- Integración con menú/toolbar.
- Materialización del guion al guardar proyecto.
- Tests base de dominio, aplicación, infraestructura y source test de UI.

## Fuera de alcance

- Editor enriquecido de texto.
- Rangos de performance.
- Asignación real de voces/personajes.
- Audio job.
- Playback.
- Storyboard bindings.

## Riesgos restantes

- Segmentos demasiado largos requieren split avanzado.
- Abrir proyecto con script asset todavía no recarga `narration-script.json` en UI.
- El editor central aún es visualización estructurada, no edición completa.
