# Tanda 42 — Storyboard simple integrado en el documento

## Objetivo

Hacer que el storyboard deje de sentirse como una cabina técnica separada y aparezca como una capa ligera del documento narrado. El mini storyboard vive en un rail plegable/retraíble y muestra miniaturas reales de imágenes importadas cuando están disponibles.

## Cambios

- Se agrega `DocumentMediaRailView` como rail documental integrado.
- El rail conserva las acciones de capa de Tanda 41 y añade secciones de:
  - mini storyboard por escenas/segmentos;
  - imágenes importadas con estado de asignación.
- Se agrega `DocumentRailImagePresentation` para distinguir imágenes asignadas a texto de imágenes sueltas.
- Al hacer clic en una escena o imagen asignada, el documento resalta el bloque de texto asociado mediante `selectDocumentBlockForStoryboardSegment`.
- Al hacer clic en una imagen sin texto asignado, la app informa que está disponible para asociarla luego.
- `DocumentWorkspaceView` usa `DocumentMediaRailView` dentro de `CollapsibleMediaRail`.
- El rail usa `ImageView` con URI de asset portable cuando existe una imagen real.
- Se agregan estilos en `css/components/media-rail.css`.

## Decisión de producto

El storyboard simple no se trata como “usuario avanzado” de entrada. Es una capa visual simple para acompañar texto/oración/párrafo con un frame representativo. El workspace Storyboard técnico sigue existiendo para revisión completa, pero la operación básica empieza en Documento.

## No incluido todavía

- Selección exacta por oración con arrastre.
- Persistencia de asignaciones por rango parcial.
- Arrastrar y soltar imágenes sobre texto.
- Exportación de video simple.
- Resolución de conflictos avanzada en UI.

## Validación esperada

```bat
scripts\02-ejecutar-tests.bat
```
