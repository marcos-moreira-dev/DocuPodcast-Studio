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

## Third-tranche engineering work

- The shell view model and shell view still expose too many category-specific operations.
- Several large JavaFX workspaces need decomposition into smaller controllers and reusable panes.
- Theatre spatial video planning and JSON persistence require narrower collaborators.
- Module exports remain broader than the intended public surface.

Sectioned JSON codecs, structured local observability, sanitized support export, neutral job recovery and the JavaFX headless profile are implemented. Direct media/document routes and the large presentation classes remain the blocking cutover work. This tranche does not add engines or redesign Narrative without stable product criteria.

The product launcher now resolves installation and writable runtime roots explicitly and has a visible-window smoke mode. Asset parity with the read-only reference is recorded in the [capability matrix](../quality/capability-parity.md). Settings publishes provider-specific, path-confined administration for the existing media adapters; missing video workflows remain an honest unavailable-resource state.

Cut evidence from 21 July 2026: `03-verificar-completo.bat` passed 895 ordinary tests (zero failures/errors, three opt-in skips), five JavaFX headless E2E tests, the reference parity audit and the visible launcher smoke. The opt-in real suite produced Piper and XTTS WAVs (unit and batch), a ComfyUI PNG, an FFmpeg MP4, OCR from native/scanned PDFs and deterministic ink evidence. Native generative video remains uninstalled because neither the reference nor the current layout contains a WAN/LTX workflow.

The GUI migration now has a build-time usage audit. Product actions, form controls and accordions use the shared visual system; accordion headers are purple with white text and expanded content is white. Short native confirmation button bars and operating-system file/folder pickers remain intentional exceptions. Experience composition has begun moving from legacy workspace lists to declared product-complexity requirements.
