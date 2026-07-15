# Referencia secundaria: Fractal Render Studio

Fractal Render Studio se leyó como referencia para procesos largos, batch, progreso y cancelación.

## Qué aporta

- `WorkerPoolManager` como patrón.
- `RenderJob` traducible a `AudioJob`.
- `RenderQueueView` traducible a `AudioJobQueueView`.
- `RenderJobStatusDto` traducible a `AudioJobStatusDto`.
- `JobCancellationToken` traducible a `AudioJobCancellationToken`.
- Carpeta por trabajo.
- Progreso por unidades.
- Cancelación cooperativa.
- Logs y métricas de duración.
- Exportación final tras unidades parciales.

## Traducción principal

```text
Fractal:
frames → PNG parciales → video final

DocuPodcast:
segmentos → WAV parciales → podcast final
```

## Qué no copiar

- Dominio fractal.
- Fórmulas, cámara, zoom fractal.
- Tema oscuro.
- Timeline fractal.
- Sesión efímera para trabajos largos.

DocuPodcast no puede borrar jobs al cerrar. Debe preservar audios parciales y permitir reanudar.

## Rol final

Fractal debe usarse como referencia de **audio jobs**, progreso, ETA, cancelación y cola batch. DMS debe usarse como referencia de UI/arquitectura general.
