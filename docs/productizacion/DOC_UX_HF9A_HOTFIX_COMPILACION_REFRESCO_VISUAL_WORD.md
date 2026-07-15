# DOC-UX-HF9A — Hotfix compilación y refresco visual Word

## Motivo

El diagnóstico completo posterior a DOC-UX-HF9 falló en `mvn compile`, `mvn test` y smoke automático porque `DocuPodcastShellViewModel` capturaba en una lambda una variable local reasignada (`storyboard`). Java exige que las variables capturadas por lambdas sean finales o efectivamente finales.

## Corrección principal

- `DocuPodcastShellViewModel.storyboardScenePresentations()` ahora usa `final StoryboardDocument storyboardForImageLayers` antes de reconstruir la proyección visual desde capas de imagen.
- La corrección no cambia el contrato visual: el rail derecho sigue reconstruyéndose desde capas de imagen actuales para reflejar imágenes asociadas a fragmentos/oraciones.

## Refresco de visuales internos de Word

También se reforzó el flujo de refresco del documento fuente:

- `SourceDocumentRefreshCoordinator` detecta cambios en metadata de bloques visuales fuente aunque el texto narrable no cambie.
- Si el refresco recupera imágenes/tablas internas del DOCX, marca el documento del proyecto como actualizado para que el usuario pueda guardarlo.
- No fuerza reinicio de audio cuando el cambio es solo metadata visual; el audio solo se invalida si el reporte indica cambio real de contenido.
- El mensaje visible ahora aclara: `Visuales internos del Word actualizados; guarda el proyecto para conservarlos.`

## Regla producto

Las imágenes/tablas del documento fuente siguen siendo bloques visuales no narrables. Solo entran al rail/storyboard cuando el usuario las asocia explícitamente como capa visual.

