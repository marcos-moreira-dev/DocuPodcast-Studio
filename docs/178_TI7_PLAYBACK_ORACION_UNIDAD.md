# 178 — TI7 Playback por oración/unidad

La tanda TI7 agrega playback unit-aware basado en `RenderUnitPlan`.

Resumen:

- `BuildPlaybackManifestUseCase` puede construir cues desde `RenderUnitPlan`.
- Las unidades TTS se resuelven desde `AudioJobSnapshot` por `RenderUnit.id`.
- Las unidades con audio humano/local se resuelven desde assets del proyecto.
- Las unidades `VISUAL_SILENT` no se reproducen como audio.
- `SeekPlaybackUseCase.seekUnit(...)` permite saltar a la unidad concreta manteniendo compatibilidad con el cursor por segmento.

Esto prepara la experiencia de reproducción por oración/unidad sin obligar a que tablas, imágenes o fórmulas no narrables sean leídas en voz.
