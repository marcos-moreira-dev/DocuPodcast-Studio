# 21 — Tests y guardarraíles

DocuPodcast debe copiar la cultura de DMS: tests de dominio, arquitectura, CSS, UI fuente, persistencia, recursos y producto.

## Tests mínimos

```text
ArchitectureBoundaryTest
NoJavaFxOutsidePresentationTest
PresentationDoesNotImportInfrastructureTest
ApplicationServicesFamilyFacadeTest
ToolbarFalsePromiseGuardTest
SideDockNoDoubleScrollTest
ScriptWorkspaceNoCanvasTest
StoryboardWorkspaceUsesCanvasTest
WordFirstProductContractTest
DocuPodcastProjectRoundTripTest
ProjectAssetRelativePathTest
AudioJobResumePersistenceTest
VoiceCapabilityPolicyTest
ExportFormatPolicyTest
GuideWordFirstTest
AiResourcesImportabilityTest
```

## Reglas

- Guion no importa canvas.
- Storyboard sí usa canvas visual.
- DOCX aparece como entrada principal.
- Toolbar no muestra acción sin handler.
- Exportación no promete formato inexistente.
- Assets usan rutas relativas.
- Audio jobs son reanudables.
- Guía menciona voces autorizadas y Word.

## Smoke manual

Debe existir checklist:

```text
Word académico → guion → audio
Guion teatral → voces/personajes → audio
Storyboard vivo → imagen por segmento → playback
Cancelación → reanudación
Exportar paquete
```
