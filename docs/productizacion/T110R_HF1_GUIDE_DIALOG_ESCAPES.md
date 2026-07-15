# T110R-HF1 — Hotfix GuideDialog escapes

Hotfix mínimo sobre T110R para corregir literales Java mal escapados en `GuideDialog.java`.

## Motivo

El diagnóstico local posterior a T110R falló durante `mvn compile` con errores `illegal escape character`, `illegal line end in character literal` y un `else without if` derivado de esos literales.

## Corrección

Se corrige el renderer ligero de la guía integrada:

- `split("\\R", -1)` para separar líneas con regex Java válido.
- `append('\n')` como literal de carácter válido.
- `matches("^\\d+\\.\\s+.+")` para listas numeradas.

## Impacto

No cambia alcance funcional. Conserva el contrato T110R:

- guía renderizada como nodos JavaFX;
- sin Markdown crudo visible;
- sin STT/Whisper en guía visible;
- documento como centro de producto.
