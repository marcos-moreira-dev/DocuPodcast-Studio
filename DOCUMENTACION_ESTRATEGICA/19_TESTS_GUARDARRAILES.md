# Tests y guardarraíles

DocuPodcast debe nacer con tests anti-promesa-falsa.

## Arquitectura

- no JavaFX fuera de presentation;
- domain no conoce application/infrastructure/presentation;
- application no conoce JavaFX;
- presentation no importa infrastructure directa.

## Producto

- Word/DOCX aparece como entrada prioritaria;
- ScriptWorkspace no usa canvas;
- StoryboardWorkspace sí usa canvas;
- toolbar no muestra acciones huérfanas;
- exportaciones se filtran por capacidad real;
- recursos IA importables tienen parser real;
- assets usan rutas relativas;
- audio job se puede reanudar.

## Tests prioritarios

- `WordFirstProductContractSourceTest`.
- `DocxDocumentImporterTest`.
- `ScriptWorkspaceNoCanvasSourceTest`.
- `StoryboardWorkspaceUsesCanvasSourceTest`.
- `AudioJobResumePersistenceTest`.
- `ToolbarFalsePromiseGuardTest`.
- `ProjectAssetRelativePathTest`.
- `GuideWordFirstTest`.
