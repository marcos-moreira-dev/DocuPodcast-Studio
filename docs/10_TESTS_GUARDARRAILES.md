# Tests y guardarraíles

## Reglas arquitectónicas

- No JavaFX fuera de presentation.
- Presentation no importa infrastructure.
- Domain no conoce application/presentation/infrastructure.
- Motor TTS detrás de gateway.

## Guardarraíles de producto

- Word/DOCX debe aparecer como entrada principal.
- Guion no usa canvas.
- Storyboard sí usa canvas.
- Toolbar no muestra acciones sin handler.
- Exportación no ofrece formatos inexistentes.
- Recursos IA no marcan plantillas incompletas como importables.
- Voz emocional solo visible si el motor la soporta.

## Tests mínimos futuros

- `WordFirstProductContractTest`
- `ScriptWorkspaceNoCanvasSourceTest`
- `StoryboardWorkspaceUsesCanvasSourceTest`
- `AudioJobResumePersistenceTest`
- `ProjectAssetRelativePathTest`
- `ToolbarFalsePromiseGuardTest`
- `GuideWordFirstTest`
