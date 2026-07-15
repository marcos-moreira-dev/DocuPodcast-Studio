# DOC-UX-HF10G — memoria, rail virtual, overlay operable y tablas sin truncado

## Motivo

La base DOC-UX-HF10F corregía parte del overlay y limpiaba visuales fuente, pero en documentos grandes seguía existiendo un riesgo real de `OutOfMemoryError` y de UI congelada. El rail derecho podía materializar una tarjeta JavaFX por cada frase y las actualizaciones de audio podían saturar la cola de JavaFX durante generación de miles de chunks.

## Cambios implementados

- `DocumentMediaRailView` usa `ListView<DocumentFragmentRailPresentation>` virtualizado en vez de un `VBox` con miles de tarjetas.
- Se agrega `AudioStatusUiThrottle` para coalescer estados de progreso antes de tocar JavaFX.
- `DocuPodcastShellViewModel` deja de reconstruir manifest por generación pura de chunks cuando no hay playback activo ni buffer pendiente.
- `SourceTableGridView` prioriza crecimiento vertical: `wrapText`, `OverrunStyle.CLIP`, altura preferida y sin etiqueta de “filas más”.
- `DocxDocumentImporter` deja de abreviar celdas de tabla a 80 caracteres y conserva todas las filas/columnas en el markdown interno de tabla.
- `scripts/01-ejecutar-app.bat` define `DOCUPODCAST_APP_HEAP=-Xmx2048m` por defecto y lo pasa al plugin JavaFX mediante `docupodcast.app.heap`.

## Resultado esperado

- El botón **Ocultar** del overlay responde durante generación larga.
- La app deja de crear miles de nodos JavaFX en el rail derecho.
- Las tablas crecen hacia abajo antes de cortar texto.
- La generación de chunks puede seguir procesándose aunque el overlay esté oculto.

## Validación recomendada

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Abrir un documento grande.
3. Generar chunks de audio desde la barra de estado.
4. Ocultar y reabrir el overlay.
5. Confirmar que el contador/ETA sigue avanzando.
6. Revisar tablas largas: deben envolver texto y expandirse verticalmente.

## Límites conocidos

Esta tanda estabiliza memoria/UI de documento grande, pero no sustituye las tandas posteriores de playback robusto, índice navegable, motores reales ni refactor transversal.
