# TI4 — Bloques visuales no narrables

## Estado tras implementación

Completado en TI4.

## Contrato aplicado

Imágenes, tablas y fórmulas son bloques visuales fuente. Se muestran en Documento como evidencia de la fuente, pero no se narran ni se asignan al storyboard automáticamente.

## Alcance

- `IMAGE_NOTICE`: imagen detectada, con render real si el DOCX entrega bytes y fallback visual si no.
- `TABLE_NOTICE`: tabla detectada, placeholder visual y conteo de filas/celdas.
- `MATH_NOTICE`: fórmula/bloque matemático Word/OMML detectado, placeholder visual. No renderiza LaTeX.

## Pendiente posterior

La asignación directa de capas a bloques no narrables y su reconciliación avanzada quedan conectadas con TI5 y con futuros ajustes de RenderUnit.
