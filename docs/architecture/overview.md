# Architecture overview

DocuPodcast Studio follows a layered local application architecture.

The capability cutover and the deliberately deferred third tranche are recorded in the [refactoring roadmap](refactoring-roadmap.md).

```mermaid
flowchart LR
    API["studio-media-api"] --> DESKTOP["studio-desktop"]
    INK["studio-ink"] --> DESKTOP
    DESKTOP --> LAUNCHER["studio-launcher"]
    ADAPTERS["studio-local-media-adapters"] --> LAUNCHER
    API --> ADAPTERS
    INK --> LAUNCHER
```

## Boundaries

- `domain` contains project, document, audio, visual, voice and theatre rules. It must not depend on JavaFX or infrastructure.
- `application` coordinates use cases and declares ports. It must not depend on JavaFX.
- `infrastructure` implements persistence, local process, media and import/export ports.
- `presentation` owns JavaFX views, view models and reusable GUI components.
- `bootstrap` is the composition root and may wire all layers.

Product-category workflows are explicit. Documentary, Narrative and Theatre may share transversal audio/video contracts, but Documentary and Narrative must not import Theatre implementation classes.

Project persistence is backward compatible. Refactors must preserve `.docupodcast.json`, relative project assets and existing example projects unless a separate migration is designed and tested.

## Long-running work

Voice synthesis, image generation, generative video and deterministic rendering use neutral contracts, shared resource scheduling, progress, cancellation and diagnostics. Views and application use cases do not invoke operating-system processes directly.

## Presentation

Reusable controls live under `presentation.components`, `presentation.ink`, `presentation.sidedock`, `presentation.ribbon` and related transversal packages. Category workspaces consume those controls instead of implementing parallel visual systems.
