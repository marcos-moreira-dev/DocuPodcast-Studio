# DOC-PERF/MEM1 — índices ligeros para documentos grandes

Base: PLAYBACK-SELECT1 / MODEL-PORT1.

## Motivo

El diagnóstico `20260606-170202.zip` quedó rojo por deuda de tamaño en `DocuPodcastShellViewModel` después de las tandas de playback/modelos. Además, la siguiente tanda pendiente era optimizar documentos grandes sin tocar motores ni la revisión final de Voz IA avanzada.

## Cambios

- Se reduce la deuda transitoria de `DocuPodcastShellViewModel` por debajo del límite RF-TX1 vigente.
- `DocumentWorkspaceView` mantiene un índice `blockIndexById` para saltos rápidos desde Índice/selección/playback en documentos grandes.
- `DocumentWorkspaceView` mantiene `sentenceSpanIndex` para estilos de selección de oraciones visibles sin volver a dividir todos los bloques del documento en cada refresco visual.
- La ventana virtualizada de documento grande se conserva: la fuente completa sigue disponible, pero JavaFX renderiza solo la ventana cercana al punto actual.

## Alcance excluido

No se toca Voz IA avanzada, descarga de modelos, playback de chunks ni render de video. La revisión final del motor de voz queda para una tanda posterior cuando haya logs de ejecución real.
