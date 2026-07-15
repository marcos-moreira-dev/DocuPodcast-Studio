# T111-HF4 — Overlay compacto, iconos grandes y fallback imagen DOCX

## Cambios

- El overlay de procesos largos se hizo más compacto y ahora tiene acción **Ocultar**.
- La barra de estado muestra **Mostrar preparación** cuando hay generación por chunks activa y el overlay está oculto.
- El overlay vuelve a mostrarse automáticamente al iniciar un nuevo job.
- Los iconos PNG del ribbon, sidebar izquierdo y rail derecho se muestran más grandes.
- Los snapshots antiguos de documento intentan recuperar imágenes embebidas desde `source/*.docx` si el `document.json` no tenía `embeddedImageBase64`.
- Se mantiene la regla de componentes transversales: botones por `ActionButtonFactory`, iconos por `IconView`/`AppIcon`, visuales por `SourceVisualBlockView`.

## Validación ChatGPT

- Revisión fuente focal de overlay, status bar, iconos y fallback DOCX.
- Diagnóstico local completo queda pendiente de ejecución en Windows.
