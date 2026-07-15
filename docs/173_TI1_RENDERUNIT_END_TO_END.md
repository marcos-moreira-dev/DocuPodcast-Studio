# Documento de tanda 173 — TI1 RenderUnit end-to-end

TI1 introduce `RenderUnit`, `RenderUnitKind`, `RenderUnitPlan` y `BuildRenderUnitPlanUseCase` como contrato central entre documento, guion, capas, audio, storyboard y video.

La tanda no reemplaza todavía audio jobs, playback ni video. Su objetivo es dejar una base ejecutable para que TI2/TI3/TI4 puedan avanzar sin seguir dependiendo exclusivamente de `NarrationSegment`.

Punto clave: el video final debe nacer de unidades con visual asignado, no de todos los segmentos. Las unidades visuales sin narración se representan como `VISUAL_SILENT` y usan duración por defecto de 5 segundos, configurable desde `OperationalSettings.VideoRenderSettings`.
