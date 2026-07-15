# 169 — Tanda 113: Ejemplos y proyectos demo internos

T113 agrega el menú **Ejemplos** y una ventana secundaria para crear proyectos demo desde recursos internos de la aplicación.

## Incluye

- `OPEN_EXAMPLE_PROJECT` en el catálogo de comandos.
- Menú superior **Ejemplos**.
- Acción **Probar ejemplo** en Inicio.
- `ExampleProjectDialog` para elegir demo.
- `ExampleApplicationServices` y `CreateExampleProjectUseCase`.
- Tres DOCX internos:
  - Instinto Creativo.
  - Cafe Luna Azul, caso contable con tabla.
  - El vuelo del Tornillo Dorado, guion teatral cómico.
- Tres imágenes PNG para el ejemplo teatral.

## Criterio UX

La selección de ejemplo no es un workspace ni una galería pesada. Es una ventana modal simple que crea el proyecto demo y deja al usuario en Documento.

## Continuidad

Después de T113 corresponde iniciar productización técnica: `RuntimePathResolver`, preflight integrado y packaging de tools/models/scripts.
