# 19 — Tema visual y CSS claro

DocuPodcast debe tener tema claro/luminoso basado en DMS, no el tema oscuro de Fractal.

## CSS modular

```text
css/
  docupodcast-light.css
  tokens.css
  shell.css
  toolbar.css
  toolbar-contextual.css
  editor-tabs.css
  statusbar.css
  workspace.css
  sidedock.css
  welcome.css
  document-reader.css
  script-workspace.css
  storyboard.css
  audio-jobs.css
  voice-library.css
  playback.css
  observability.css
  guide.css
  dialogs.css
  operational-help.css
```

## Tokens

Prefijo `-docu-*`.

Estados visuales importantes:

```text
audio-state-pending
audio-state-generating
audio-state-completed
audio-state-failed
script-voice-chip
script-style-chip
storyboard-scene-active
generation-progress-eta
```

## Estética

- Desktop sobrio.
- Lectura cómoda.
- Paneles claros.
- Bordes discretos.
- Botones sin radius o radius mínimo.
- Barra de progreso muy visible.

## Regla

No heredar nombres `diagram-*`.
