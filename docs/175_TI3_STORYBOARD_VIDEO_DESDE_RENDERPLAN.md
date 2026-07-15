# 175 — TI3 Storyboard/video desde RenderPlan

TI3 cambia el origen conceptual del storyboard/video: ya no debe depender de todos los segmentos del guion, sino de `RenderUnitPlan.videoUnits()`.

Esto permite que DocuPodcast diferencie unidades habladas sin visual, unidades habladas con visual y unidades visuales silenciosas. La exportación conserva fallback legacy para proyectos antiguos.
