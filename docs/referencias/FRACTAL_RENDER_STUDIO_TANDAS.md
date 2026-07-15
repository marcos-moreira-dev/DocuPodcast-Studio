# Lecturas estratégicas de Fractal Render Studio

## Valor principal

Fractal es referencia para jobs largos y procesamiento por lotes.

## Piezas reutilizables

- `WorkerPoolManager` como patrón.
- Job con estado mutable y timestamps.
- Cola visible.
- Cancelación cooperativa.
- Progreso por unidades.
- Métricas de duración.
- Carpeta por job.
- Exportación final después de procesar unidades.

## Traducción a DocuPodcast

```text
RenderJob       → AudioJob
Frame           → NarrationSegment
PNG parcial     → WAV parcial
MP4 final       → podcast WAV/MP3
RenderQueueView → AudioJobQueueView
```

## Diferencias

DocuPodcast no debe copiar sesión efímera de Fractal. Los jobs de audio deben persistirse y reanudarse.

Fractal tiene tema oscuro. DocuPodcast debe usar tema claro inspirado en DMS.
