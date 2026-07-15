# Tanda 95 — Catálogo único de comandos

T95 agrega el catálogo y dispatcher base de comandos de presentación antes del rediseño fuerte de GUI.

## Archivos principales

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandDispatcher.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/WorkspaceCapabilityCommandMapper.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarAction.java`

## Guardarraíl

El rediseño de menú/ribbon/sidebar no debe hardcodear acciones nuevas sin `AppCommandId`.
