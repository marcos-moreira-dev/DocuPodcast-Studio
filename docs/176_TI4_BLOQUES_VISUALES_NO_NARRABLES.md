# 176 — TI4 Bloques visuales no narrables

TI4 formaliza imágenes, tablas y fórmulas como bloques visuales fuente no narrables por defecto.

- `IMAGE_NOTICE`, `TABLE_NOTICE` y `MATH_NOTICE` son `sourceVisual()`.
- No se narran automáticamente.
- No entran al storyboard automáticamente.
- Word/OMML/LaTeX se identifica como fórmula, pero no se renderiza LaTeX en esta tanda.
- El documento usa `SourceVisualBlockView` para mostrar imagen real si existe o placeholder visual si es tabla/fórmula/imagen sin bytes.
