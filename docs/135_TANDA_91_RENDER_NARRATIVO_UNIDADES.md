# Tanda 91 implementada — Render narrativo por unidades

T91 introduce `NarrationRenderPlan` y `NarrationRenderUnit` para representar unidades finas de narración, normalmente oraciones, con voz, emoción, audio clip e imagen efectivos.

La tanda prepara el cerebro para que una selección de oración/rango pueda gobernar audio y storyboard sin depender únicamente del `segmentId`. El playback queda preparado con `unitId`, pero T91 no reemplaza todavía los jobs TTS por segmento.

Ver: `docs/productizacion/T91_RENDER_NARRATIVO_UNIDADES.md`.
