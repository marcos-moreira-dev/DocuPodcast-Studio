# T111-HF2 — Iconos PNG y refuerzo de imagen DOCX

Hotfix aplicado sobre T111.

## Motivo

La primera versión de T111 reemplazó marcadores temporales por glifos semánticos, pero visualmente seguían viéndose demasiado simples. Además, el smoke visual con *Instinto Creativo* seguía mostrando el bloque de imagen fuente como aviso textual cuando la imagen embebida no llegaba a la vista.

## Cambios

- Agregado catálogo `AppIcon` con recursos PNG en `src/main/resources/icons/ui/`.
- Agregado componente transversal `IconView` para renderizar iconos PNG en Ribbon, sidebar izquierdo y rail derecho.
- `RibbonButton` acepta `AppIcon` y sigue preservando constructores heredados.
- `RibbonIconCatalog` ahora devuelve iconos semánticos PNG, no glifos temporales.
- `WorkspaceSideDock` usa iconos transversales para Texto, Audio e Imagen.
- `RailToggleButton` usa icono transversal para colapsar/expandir el rail.
- `DocxDocumentImporter` añade fallback para recuperar imágenes desde `word/media` si el bloque detectado no recibió `embeddedImageBase64`.
- `SourceVisualBlockView` mantiene el render embebido como componente transversal y fija alto razonable para previsualizar imágenes grandes.
- Source tests T111 alineados con la nueva playbar guiada y el catálogo PNG.

## Regla preservada

Las imágenes detectadas en el Word se muestran como parte del documento fuente, pero no se asignan automáticamente al storyboard. La asignación visual sigue siendo decisión del usuario.
