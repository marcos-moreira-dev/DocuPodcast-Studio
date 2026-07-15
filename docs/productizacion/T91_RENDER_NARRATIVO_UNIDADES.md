# T91 — Render narrativo por unidades de oración/rango

## Estado

Implementada sobre T90FG.

T91 introduce el contrato de cerebro para que el documento pueda operar por unidad narrable fina, normalmente oración, sin romper la compatibilidad con los jobs de audio existentes por segmento.

## Problema que corrige

Hasta T90G la interfaz permitía seleccionar una oración o rango y guardar capas de voz, emoción, imagen o audio del computador, pero el audio seguía conceptualizado principalmente como:

```text
NarrationSegment → WAV por segmentId → PlaybackCue por segmentId
```

Eso impedía razonar de forma limpia sobre:

```text
oración seleccionada → voz efectiva / emoción / audio clip / imagen → unidad renderizable
```

## Nuevos contratos

### Dominio

```text
com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind
com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit
com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan
```

Cada `NarrationRenderUnit` conserva:

```text
unitId
segmentId
index
scriptRange
documentRange opcional
texto efectivo
sourceKind: TEXT_TO_SPEECH o AUDIO_CLIP
voiceProfileId efectivo
performanceStyleId efectivo
audioAssetId efectivo
imageAssetId efectivo
appliedLayerIds
```

### Aplicación

```text
com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase
com.marcosmoreiradev.docupodcaststudio.application.services.RenderApplicationServices
```

El caso de uso toma el guion y las capas del proyecto, divide cada segmento narrable en oraciones mediante el splitter determinístico existente y resuelve capas efectivas por superposición de rango.

## Reglas T91

- `VOICE` cambia la voz efectiva de la unidad.
- `EMOTION` cambia el estilo/performance efectivo.
- `HUMAN_AUDIO` convierte la unidad en `AUDIO_CLIP` y usa el `audioAssetId` asignado.
- `IMAGE` asocia la imagen efectiva de la unidad.
- `Audio del computador` sigue siendo clip genérico: puede ser voz, pájaros, efecto, música, ruido o cualquier audio elegido por el usuario.

## Playback preparado para unidades

`PlaybackCue` ahora tiene `unitId`, manteniendo constructores compatibles por segmento. `PlaybackManifest` permite múltiples cues del mismo `segmentId` siempre que cada `unitId` sea único.

Esto prepara T92, donde el audio del computador asignado a una unidad deberá afectar playback/export de verdad.

## Alcance deliberadamente no incluido

T91 no reemplaza todavía los jobs TTS por segmento. Los jobs existentes siguen siendo compatibles. Esta tanda introduce el plan y el identificador de unidad para que la siguiente tanda pueda conectar unidades reales a playback/export sin romper todo el audio existente.

T91 tampoco introduce `TextAnchor` persistente ni migración de formato; eso queda para T93.

## Tests agregados

```text
NarrationRenderPlanTest
BuildNarrationRenderPlanUseCaseTest
NarrationRenderUnitsT91SourceTest
```

También se amplía `PlaybackManifestTest` para aceptar múltiples cues por segmento si los `unitId` son únicos.

## Próxima tanda recomendada

T92 — Audio del computador efectivo en playback/export.
