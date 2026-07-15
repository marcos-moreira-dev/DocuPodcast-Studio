# Tests y guardarraíles

DocuPodcast debe nacer con tests de arquitectura, producto y UI fuente.

## Arquitectura

- `ArchitectureBoundaryTest`.
- `NoJavaFxOutsidePresentationTest`.
- `PresentationDoesNotImportInfrastructureTest`.
- `ApplicationServicesFamilyFacadeTest`.

## Producto

- `WordFirstProductContractSourceTest`.
- `ToolbarFalsePromiseGuardTest`.
- `GuideWordFirstTest`.
- `VoiceConsentHelpSourceTest`.

## Workspaces

- `ScriptWorkspaceNoCanvasSourceTest`.
- `StoryboardWorkspaceUsesCanvasSourceTest`.
- `SideDockNoDoubleScrollSourceTest`.
- `AudioProgressUiContractSourceTest`.

## Persistencia

- `DocuPodcastProjectRoundTripTest`.
- `ProjectAssetRelativePathTest`.
- `AudioJobResumePersistenceTest`.

## Importación

- `DocxDocumentImporterTest`.
- `NarrationScriptMarkdownParserTest`.

## Audio

- `AudioJobProgressTest`.
- `AudioJobResumeTest`.
- `AudioExportSafetyPolicyTest`.

## Principio

Todo botón visible debe tener cadena real y test mínimo.
