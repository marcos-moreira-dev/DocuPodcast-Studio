# Referencias DMS + Fractal

## Domain Model Studio/UENS

Usar como base conceptual para:

- `ApplicationBootstrap` y `ApplicationRuntime`;
- shell multiproyecto;
- tabs reordenables;
- toolbar global/contextual;
- SideDock modular;
- workspaces estructurados/visuales;
- tema claro tokenizado;
- guía integrada;
- `.dms` como referencia de persistencia JSON;
- Markdown/Recursos IA;
- exportación por artefacto;
- tests y documentación viva.

## Fractal Render Studio

Usar como base conceptual para:

- `WorkerPoolManager`;
- job por unidades;
- cola de trabajos;
- cancelación cooperativa;
- progreso;
- métricas;
- carpeta de output por job.

## Traducción de conceptos

| Fractal | DocuPodcast |
|---|---|
| RenderJob | AudioJob |
| frame | segmento narrable |
| PNG parcial | WAV parcial |
| MP4 final | podcast final |
| RenderQueueView | AudioJobQueueView |

| DMS | DocuPodcast |
|---|---|
| `.dms` | `.docupodcast.json` |
| DiagramToolbar | WorkspaceToolbar |
| StructuredWorkbench | StructuredWorkspace |
| DiagramWorkbench | VisualWorkspace / StoryboardWorkspace |
| SideDock | SideDock contextual |
