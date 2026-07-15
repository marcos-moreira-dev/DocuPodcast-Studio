# Current product status

DocuPodcast Studio is a Windows desktop application with three active project categories.

## Documentary Studio

- Imports DOCX/PDF content and preserves document structure.
- Supports narration, visual assignments, technical-problem canvases and final audio/video export.
- Documentary video preparation is independent from Theatre audio-track infrastructure.

## Narrative Video

- Builds narrated visual timelines from project script and media assignments.
- Uses transversal video rendering without Theatre-specific dependencies.

## Theatre

- Models acts, scenes, interventions, characters, voices and visual context.
- Supports generated/assigned/storyboard images, camera references, stage backdrops and intermediate frames.
- Supports scoped theatrical exports and local audio/image workflows.

## Known engineering debt

- The shell view model and shell view still expose too many category-specific operations.
- Several large JavaFX workspaces need decomposition into smaller controllers and reusable panes.
- Theatre spatial video planning and JSON persistence require narrower collaborators.
- Module exports remain broader than the intended public surface.

These items are addressed incrementally; behavior and project schema remain stable during refactoring.

