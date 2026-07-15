# CSS y tema claro

DocuPodcast debe usar tema claro, profesional y legible, inspirado en DMS, no el tema oscuro de Fractal.

## Archivos CSS previstos

```text
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

- fondos claros;
- paneles blancos/grises;
- texto gris carbón;
- acento azul;
- éxito verde;
- advertencia ámbar;
- error rojo.

## Estados visuales importantes

- audio pendiente/generando/completado/fallido;
- segmento seleccionado;
- segmento en reproducción;
- escena activa;
- voz propia/autorizada/prediseñada;
- warnings de motor TTS.

## Regla

No heredar nombres `diagram-*` en CSS nuevo.
