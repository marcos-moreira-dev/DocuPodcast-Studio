# Tanda 81E — Sidebar derecho de miniaturas y medios

## Propósito

T81E convierte el rail derecho del workspace Documento en una superficie **visual y navegacional**, no en un panel de botones ni en una segunda cabina técnica. El objetivo de producto es que el usuario vea rápidamente qué medios están asociados al documento —capas, frames de storyboard e imágenes disponibles— y pueda volver al fragmento relacionado con un clic.

Esta tanda parte de T81D-HF1 verde. No cambia el cerebro V1 congelado. Tampoco agrega nuevas capacidades de media: reorganiza la presentación del rail derecho para respetar la arquitectura visual decidida en T81.

## Decisión UX

La estructura visual principal queda:

```text
[Inspector izquierdo contextual] [Workspace Documento] [Rail derecho visual]
```

- El inspector izquierdo concentra acciones repetitivas por fragmento: Detalles, Audio / Narración e Imagen.
- El centro conserva la hoja/documento y la barra flotante de lectura global.
- El rail derecho queda como lista visual: miniatura, fragmento relacionado y breve descripción.

## Qué cambió

### 1. `DocumentMediaRailView` deja de construir tarjetas visuales ad-hoc

Antes el rail construía manualmente tarjetas con `HBox`, `StackPane`, `ImageView`, múltiples labels y textos de ayuda extensos dentro de la propia vista documental.

Ahora `DocumentMediaRailView` usa el componente transversal:

```text
MediaThumbnailCard
```

Esto evita repetir estructura visual dentro del workspace y facilita que futuras superficies de miniaturas usen el mismo lenguaje.

### 2. Nuevo componente transversal `MediaThumbnailCard`

Archivo:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/MediaThumbnailCard.java
```

Responsabilidad:

- renderizar miniatura real cuando hay `imageFileUri`;
- mostrar placeholder cuando no hay imagen;
- mostrar título;
- mostrar fragmento/relación;
- mostrar descripción breve;
- ejecutar una acción de navegación al hacer clic;
- instalar tooltip de navegación;
- usar clases de estilo centralizadas en `AppStyles`.

No es un botón de asignación. Es una tarjeta visual/navegacional.

### 3. Nuevos estilos compartidos

`AppStyles` agrega:

```text
UI_MEDIA_THUMBNAIL_CARD
UI_MEDIA_THUMBNAIL
UI_MEDIA_THUMBNAIL_LABEL
UI_MEDIA_CARD_COPY
UI_MEDIA_CARD_TITLE
UI_MEDIA_CARD_RELATION
UI_MEDIA_CARD_DESCRIPTION
```

`media-rail.css` agrega estilos para:

```text
.ui-media-thumbnail-card
.ui-media-thumbnail
.ui-media-thumbnail-label
.ui-media-card-title
.ui-media-card-relation
.ui-media-card-description
.document-media-assignment-card
```

### 4. El rail derecho queda dividido en tres listas simples

`DocumentMediaRailView` expone tres secciones visuales:

```text
Medios asignados
Mini storyboard
Imágenes disponibles
```

Cada tarjeta muestra solo:

```text
miniatura o placeholder
fragmento / relación
breve descripción
```

La intención es evitar textos largos y formularios dentro del rail.

## Conducta por sección

### Medios asignados

Se alimenta de:

```text
viewModel.documentLayerAssignmentPresentations()
```

Sirve para mostrar capas ya guardadas. Al hacer clic:

```text
viewModel.selectNarrativeLayerAssignment(assignment.id())
```

El objetivo es regresar al texto o selección asociada.

### Mini storyboard

Se alimenta de:

```text
viewModel.storyboardScenePresentations()
```

Muestra frames representativos. Al hacer clic:

```text
viewModel.selectDocumentBlockForStoryboardSegment(scene.segmentId())
```

El documento debe navegar/resaltar el bloque asociado.

### Imágenes disponibles

Se alimenta de:

```text
viewModel.documentRailImagePresentations()
```

- Si la imagen está asignada, clic navega al segmento asociado.
- Si la imagen está sin texto asignado, clic la selecciona como imagen suelta para una asociación posterior.

```text
viewModel.selectDocumentBlockForStoryboardSegment(image.assignedSegmentId())
viewModel.selectLooseStoryboardImage(image.assetId())
```

## Qué NO debe volver al rail derecho

El rail derecho no debe volver a exponer acciones como:

```text
Asignar voz IA
Asignar audio del computador
Asignar emoción
Asociar imagen / storyboard
Agregar audio ambiente
Elegir audio
Extraer audio de video
```

Esas acciones pertenecen al inspector izquierdo contextual, especialmente al módulo `Audio / Narración` o al módulo `Imagen`.

## Refinamiento incluido en la barra flotante

Aprovechando esta tanda, se ajustó la barra flotante de lectura global para que el texto explicativo de la acción principal vaya al tooltip del botón y no ocupe espacio horizontal permanente.

`PrimaryActionStrip` conserva compatibilidad con el modo anterior, pero agrega un constructor con `showHintText`:

```text
PrimaryActionStrip(actionLabel, hint, action, false)
```

`FloatingReadingControlBar` usa ese modo para que la barra respire y no comprima los botones Pausar, Reanudar, Detener y Refrescar contenido.

## Guardarraíles

Se agregan/actualizan source tests para asegurar que:

- `DocumentMediaRailView` usa `MediaThumbnailCard`;
- el rail derecho no contiene acciones de asignación;
- el componente visual usa `ImageView` y tooltip;
- el rail conserva navegación al documento;
- las imágenes asignadas y no asignadas siguen diferenciadas;
- los estilos viven en CSS modular.

## Próximo paso

La siguiente tanda recomendada es:

```text
T81F — Toolbar con iconos y grupos
```

Ahí se debe atacar la barra superior que todavía conserva acciones textuales técnicas como `Narración avanzada`, `Voces` y `Audio`.
