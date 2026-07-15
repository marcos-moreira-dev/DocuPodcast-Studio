# TI1 — RenderUnit end-to-end

## Propósito

TI1 introduce el contrato central que debe unir documento, guion, capas, audio, storyboard y video sin seguir forzando que todo nazca de un `NarrationSegment`.

Hasta TP6 el proyecto tenía piezas reales y fuertes: documento importado, guion narrable, capas narrativas, `NarrationRenderPlan`, jobs de audio, playback manifest, storyboard y paquete de video. La deuda principal era que esas piezas todavía giraban alrededor del segmento narrable. Eso impedía representar correctamente bloques fuente no narrables: imágenes del documento, tablas y futuras fórmulas/LaTeX.

TI1 agrega un nivel más explícito:

```text
Documento + guion + capas
→ NarrationRenderPlan
→ RenderUnitPlan
→ RenderUnit
```

Cada `RenderUnit` declara si corresponde a:

- audio solamente;
- audio + visual;
- visual silencioso;
- unidad omitida del video.

## Alcance exacto de TI1

TI1 NO reemplaza todavía los jobs de audio, el playback ni el video legacy. Eso queda para TI2 y TI3.

TI1 SÍ deja el contrato de dominio/aplicación para que esas tandas tengan una base estable.

## Nuevos conceptos

### `RenderUnitKind`

Estados:

- `SPOKEN_ONLY`: unidad narrada sin visual asignado. Sirve para audio/playback, pero no debe producir frame de video.
- `SPOKEN_WITH_VISUAL`: unidad narrada con visual asignado. Puede producir imagen + audio en video.
- `VISUAL_SILENT`: unidad visual no narrable. Puede producir frame silencioso con duración configurada.
- `OMITTED`: unidad omitida de render visual.

### `RenderUnit`

Representa la decisión final de render para una unidad efectiva.

Campos clave:

- `id`;
- `sourceNarrationUnitId`;
- `segmentId`;
- `scriptRange`;
- `documentRange`;
- `text`;
- `kind`;
- `voiceProfileId`;
- `performanceStyleId`;
- `audioAssetId`;
- `imageAssetId`;
- `silentDurationSeconds`;
- `appliedLayerIds`.

Reglas de validación:

- una unidad hablada requiere `segmentId`, `scriptRange` y texto;
- una unidad visual requiere `imageAssetId`;
- una unidad visual silenciosa requiere `documentRange`;
- la duración silenciosa queda limitada entre 1 y 60 segundos;
- si no se define duración, usa 5 segundos por defecto.

### `RenderUnitPlan`

Agrupa las unidades y expone métricas:

- `unitCount()`;
- `spokenUnitCount()`;
- `visualUnitCount()`;
- `silentVisualUnitCount()`;
- `omittedFromVideoCount()`;
- `audioUnits()`;
- `videoUnits()`.

### `BuildRenderUnitPlanUseCase`

Convierte un `NarrationRenderPlan` en `RenderUnitPlan`.

Reglas iniciales:

- `NarrationRenderUnit` con imagen → `SPOKEN_WITH_VISUAL`;
- `NarrationRenderUnit` sin imagen → `SPOKEN_ONLY`;
- permite añadir unidades `VISUAL_SILENT` ya construidas para bloques fuente no narrables;
- puede tomar la duración silenciosa desde `OperationalSettings.VideoRenderSettings.silentVisualBlockSeconds()`.

## Relación con la regla de producto

Regla protegida:

```text
Una imagen, tabla o fórmula detectada dentro del documento fuente no entra automáticamente al storyboard ni al video.
```

Con TI1, el camino correcto queda preparado:

```text
Bloque fuente visual
→ si el usuario no asigna visual: omitido
→ si el usuario asigna visual: RenderUnit VISUAL_SILENT
→ duración por defecto: 5 segundos
→ duración configurable en Configuración
```

## Qué NO se debe hacer después de TI1

- No volver a hacer que todo video nazca de todos los segmentos narrables.
- No tratar `IMAGE_NOTICE` o `TABLE_NOTICE` como texto a narrar por defecto.
- No asumir que una imagen embebida del Word ya es una imagen de storyboard.
- No romper los jobs actuales hasta que TI2 esté listo.
- No reemplazar `NarrationRenderPlan` sin migración y tests.

## Qué sigue

TI2 debe hacer que los jobs de audio consuman `RenderUnitPlan`.

TI3 debe hacer que storyboard/video consuman `RenderUnitPlan`.

TI4 debe cerrar la creación real de unidades `VISUAL_SILENT` desde imágenes/tablas/LaTeX fuente.
