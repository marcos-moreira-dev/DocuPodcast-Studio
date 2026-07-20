# Current product status

Architecture delivery and the scope reserved for the next cleanup are tracked in the [refactoring roadmap](../architecture/refactoring-roadmap.md).

DocuPodcast Studio is a Windows desktop application with three active project categories.

## Documentary Studio

- Imports DOCX/PDF content and preserves document structure.
- Supports narration, visual assignments, technical-problem canvases and final audio/video export.
- Documentary video preparation is independent from Theatre audio-track infrastructure.

## Narrative Video

- Builds narrated visual timelines from project script and media assignments.
- Uses transversal video rendering without Theatre-specific dependencies.
- Native generative-video infrastructure is available as a separate capability; product composition remains `EVOLVING` until its criteria stabilize.

## Theatre

- Models acts, scenes, interventions, characters, voices and visual context.
- Supports generated/assigned/storyboard images, camera references, stage backdrops and intermediate frames.
- Supports scoped theatrical exports and local audio/image workflows.

## Third-tranche engineering debt

- The shell view model and shell view still expose too many category-specific operations.
- Several large JavaFX workspaces need decomposition into smaller controllers and reusable panes.
- Theatre spatial video planning and JSON persistence require narrower collaborators.
- Module exports remain broader than the intended public surface.

The third tranche also covers sectioned JSON codecs, remaining OCR/document I/O, structured observability, JavaFX/E2E accessibility and abrupt-close recovery. It will not add engines or redesign Narrative without stable product criteria.
