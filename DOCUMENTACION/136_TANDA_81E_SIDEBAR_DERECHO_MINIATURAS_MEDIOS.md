# Tanda 81E — Sidebar derecho de miniaturas y medios

Esta tanda transforma el rail derecho de Documento en una lista visual y navegacional. Las acciones de asignación ya viven en el inspector izquierdo contextual, por lo que el rail derecho deja de actuar como panel mixto de instrucciones, botones y capas.

## Archivos principales

```text
DocumentMediaRailView.java
MediaThumbnailCard.java
AppStyles.java
media-rail.css
```

## Resultado

El rail derecho muestra medios asignados, mini storyboard e imágenes disponibles mediante tarjetas compactas. Cada tarjeta muestra miniatura/placeholder, fragmento relacionado y descripción breve. Al hacer clic, la app navega al texto asociado o selecciona una imagen suelta.

## Regla

No se deben introducir botones de asignación en el rail derecho. Elegir audio, extraer audio de video, emoción/estilo e imagen pertenecen al inspector izquierdo.
