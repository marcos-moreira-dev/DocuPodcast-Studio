# 05 — Referencia Fractal Render Studio

Fractal Render Studio fue leído como referencia de procesos largos, progreso y generación batch. No se debe copiar su dominio fractal, pero sí sus patrones de ejecución.

## Qué tomar

- `ApplicationBootstrap`, configuración y empaquetado como referencia secundaria.
- Cola de trabajos largos.
- `WorkerPoolManager` como patrón.
- `RenderJob` → `AudioJob`.
- `RenderJobStatusDto` → `AudioJobStatusDto`.
- `RenderQueueView` → `AudioJobQueueView`.
- Cancelación cooperativa.
- Carpeta por job.
- Métricas de duración.
- Exportación por unidades.
- Logs visibles.

## Traducción conceptual

```text
Fractal:
frames → PNG parciales → MP4 final

DocuPodcast:
segmentos → WAV parciales → podcast final
```

## Mejoras necesarias para DocuPodcast

Fractal puede tener cola en memoria y sesión efímera. DocuPodcast no. Los jobs de audio deben persistir porque pueden tardar minutos y fallar en el segmento 80 de 120.

DocuPodcast necesita además:

- ETA.
- Segmento actual.
- Fallidos.
- Reintentos.
- Reanudación.
- `job.json`.
- `segments-status.json`.
- `audio-manifest.json`.
- `generation-log.jsonl`.

## Qué no tomar

- Tema oscuro.
- Dominio fractal.
- Renderers matemáticos.
- Cámara/zoom fractal.
- Sesión efímera.
- Timeline fractal.
