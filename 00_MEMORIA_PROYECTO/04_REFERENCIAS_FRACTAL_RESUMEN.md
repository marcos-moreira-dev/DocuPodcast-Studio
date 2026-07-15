# Referencia estratégica: Fractal Render Studio

Fractal Render Studio fue leído por tandas para extraer patrones de procesos largos, cola, progreso y generación batch.

## Qué aporta a DocuPodcast

Fractal es la referencia principal para:

- Trabajos largos en segundo plano.
- `WorkerPoolManager` como patrón.
- `RenderJob` como base conceptual de `AudioJob`.
- `RenderQueueView` como base conceptual de `AudioJobQueueView`.
- `RenderJobStatusDto` como base conceptual de `AudioJobStatusDto`.
- Cancelación cooperativa.
- Progreso por unidades.
- Carpeta reproducible por job.
- Logs visibles.
- Métricas de duración.

## Traducción conceptual

| Fractal | DocuPodcast |
|---|---|
| Render job | Audio generation job |
| Frame | Narration segment |
| PNG parcial | WAV parcial |
| MP4 final | WAV/MP3 final |
| Render queue | Audio job queue |
| Render progress | Audio progress + ETA |
| Cancel render | Cancel audio job safely |

## Qué no debe copiarse

- Tema oscuro.
- Dominio fractal.
- Cámara, zoom fractal, fórmulas.
- Renderers matemáticos.
- Sesión efímera para jobs.

## Regla para DocuPodcast

Un documento largo nunca se procesa como una sola operación. Se convierte en segmentos, se genera audio por segmento, se guarda el progreso y se puede reanudar o reintentar.
