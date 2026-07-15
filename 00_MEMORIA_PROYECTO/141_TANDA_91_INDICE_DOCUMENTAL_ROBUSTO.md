# Tanda 91 - Indice documental robusto para libros grandes

## Que se implemento

- Se agrego un modelo de outline en `application.document`: `DocumentOutlineEntry`, `DocumentOutlineProjection`, `DocumentOutlineOrigin`, `DocumentOutlineHint`, `DocumentOutlineHintProvider`.
- Se agrego `BuildDocumentOutlineUseCase` para construir una proyeccion de navegacion sin mutar el documento ni el `.docupodcast.json`.
- El orden de deteccion quedo asi: bookmarks PDF, tabla de contenidos, headings existentes, secciones inferidas y fallback plano.
- Se agrego `PdfBookmarkOutlineHintProvider` en infraestructura como lector best-effort para bookmarks PDF simples/no cifrados.
- `DocumentIndexPanel` ahora solo renderiza `DocumentOutlineProjection`; ya no decide estructura con `hasStructuralHeadings()`, `structuralRoot` o `flatRoot`.
- El SideDock muestra el origen del indice en el encabezado.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Mejoras visuales profundas del panel de indice.
- Gestor/listado de problemas guardados.
- Persistencia nueva en `.docupodcast.json`.

## Decisiones tecnicas

- El indice es una proyeccion en memoria de application; no cambia el documento fuente.
- Los bookmarks PDF son opcionales y fallan cerrado: si no se pueden leer o mapear a pagina, el lector cae a Contents/heuristicas/fallback.
- Las paginas Contents se detectan en las primeras paginas y soportan entradas en una linea o partidas con numero de pagina en la siguiente linea.
- Para Contents, el mapeo prefiere buscar texto real posterior al indice; `sourcePage` impreso queda como aproximacion secundaria.
- Si el Contents empieza en `1.1` sin entrada padre `1`, los niveles se normalizan para que esas entradas puedan aparecer como raices.

## Archivos y sistemas tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/*Outline*.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildDocumentOutlineUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBookmarkOutlineHintProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- Tests de application/infrastructure/presentation/productizacion.
- Docs de indice documental y PDF.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildDocumentOutlineUseCaseTest,PdfBookmarkOutlineHintProviderTest,DocumentIndexHf13SourceTest,DocIndexUxHf1SourceTest,ArchitectureBoundaryTest,VoiceChunksGpuStatusHf1SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 92: gestor/listado de problemas guardados en SideDock para reabrir, editar y exportar soluciones.
2. Agregar acciones por problema: abrir, renombrar, duplicar, eliminar y exportar solucion.
3. Despues, mejorar diagnostico visual del indice PDF mostrando cuando el origen fue fallback plano por baja confianza.
