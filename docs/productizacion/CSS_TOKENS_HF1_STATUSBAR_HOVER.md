# CSS-TOKENS-HF1 — Token CSS faltante y hover pastel del status bar

## Objetivo

Eliminar el warning JavaFX por `-dp-ink` y mantener los botones del status bar con hover morado pastel claro, sin fondo oscuro ni comportamiento visual protagonista.

## Cambios

- `tokens.css` define `-dp-ink` como alias compatible de texto principal.
- `tokens.css` define `-docu-status-hover-pastel`.
- `statusbar.css` usa hover pastel en botones de proceso, generación y tamaño de lectura.

## Criterio UX

El status bar es una superficie operativa secundaria. Sus botones deben indicar hover de forma suave y clara, sin competir visualmente con Documento ni Ribbon.
