# Tanda 35A — Documento narrado como experiencia principal

## Propósito

Esta tanda no agrega un motor nuevo ni reemplaza el modelo existente. Alinea la interfaz con la visión de producto confirmada después de Tanda 34C verde: DocuPodcast debe sentirse primero como un lector narrado de documentos Word y no como una cabina técnica de guion/audio/storyboard.

## Decisiones fijadas

- El DOCX original sigue siendo fuente importada inmutable.
- `ReadableDocument` y `DocumentBlock` siguen siendo la base normalizada del documento.
- Las asignaciones de voz, audio, emoción, imagen y storyboard deben guardarse como capas del proyecto DocuPodcast, no dentro del Word.
- La pantalla principal debe permitir leer/escuchar el documento; las vistas técnicas de guion, audio, voces y storyboard quedan como capas o configuración avanzada.
- El futuro botón de reproducción debe comportarse de forma inteligente: si no hay selección, reproduce el documento; si hay cursor/selección, reproduce desde ahí.
- No se deben llenar las pantallas con botones duplicados. La selección del texto y el contexto deben guiar la acción principal.

## Cambios visibles

- Welcome reorientado a “lector narrado local para documentos Word”.
- El workspace Documento se presenta como “Documento narrado”.
- Los bloques se renderizan con una columna sutil de marcadores, preparando la futura asociación de voz/audio/emoción/imagen.
- El SideDock ahora puede ocultar el módulo activo y dejar visible solo la barra lateral compacta.
- El panel Documento usa más espacio central y menos anchura inicial para el SideDock.

## Alcance no incluido todavía

No se implementa aún:

- selección real por oración con arrastre;
- `PerformanceSpan` creado desde una selección parcial de texto;
- asignación de voz/audio/imagen desde menú contextual;
- botón inteligente real “Reproducir documento / Reproducir desde aquí”;
- exportación de video simple.

Estos puntos quedan como siguiente etapa funcional sobre la base ya existente: `ScriptTextRange`, `PerformanceSpan`, `NarrationSegment`, `AudioJob` y `StoryboardScene`.
