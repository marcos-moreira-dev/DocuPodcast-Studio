# DOC-INDEX-HF13 - Indice navegable del documento

Estado: implementada y ampliada en Tanda 91.

## Motivo

El lector principal necesita navegacion limpia para documentos largos con titulos, subtitulos, secciones o tablas de contenido. La solucion no debe volver al panel tecnico antiguo de estructura documental ni convertir el lector en dashboard: debe ser un indice lateral plegable, contextual y facil de ocultar.

## Cambios principales

- `DocumentIndexPanel` es el panel del workspace Documento para el modulo lateral `Indice`.
- `DocumentWorkspaceView` registra el modulo junto a `Fragmento`, `Audio` e `Imagen`.
- El indice usa `TreeView<IndexEntry>` para pintar una proyeccion calculada en `application.document`.
- Desde Tanda 91, `DocumentIndexPanel` no decide la estructura: consume `DocumentOutlineProjection` generado por `BuildDocumentOutlineUseCase`.
- El origen del indice se muestra en el encabezado del SideDock: bookmarks PDF, tabla de contenidos, titulos detectados, secciones inferidas o navegacion plana.
- Al hacer clic en una entrada, se llama a `selectBlock(...)` y el lector salta al bloque, incluso si el documento grande requiere cambiar la ventana virtualizada.
- Si el documento PDF no tiene indice confiable, el sistema usa navegacion por paginas antes de caer a navegacion plana por bloques narrables y visuales.
- Desde Tanda 95, el fallback por paginas usa `sourcePageCount` para mostrar todas las paginas conocidas del PDF; las paginas sin bloque exacto apuntan al bloque util mas cercano.
- Desde Tanda 96, una inferencia pobre en PDFs grandes ya no gana automaticamente: si detecta pocas entradas, estan concentradas o dominadas por bibliografia/referencias, el indice cae a paginas PDF.
- El panel queda plegable dentro de `WorkspaceSideDock`; no se reintroduce el panel viejo `Estructura documental` como superficie principal.

## Orden de deteccion Tanda 91

Para PDFs se intenta construir la navegacion en este orden:

1. Bookmarks PDF best-effort mediante `PdfBookmarkOutlineHintProvider`, solo para PDFs no cifrados y cuando se puede mapear a pagina.
2. Paginas `Contents` o `Table of Contents` en las primeras paginas, con entradas normales o partidas en varias lineas.
3. Titulos/encabezados existentes del documento normalizado.
4. Secciones inferidas por patrones de libro tecnico: `Chapter 1`, `Appendix A`, `Preface`, `1`, `1.1`, `1.1.1`; desde Tanda 96 deben superar una validacion minima de confianza en PDFs grandes.
5. Anclas por paginas PDF cuando existe `sourcePage`/`sourcePageCount`.
6. Fallback plano por bloques narrables o visuales.

Las entradas de Contents se mapean primero por texto cercano despues de la pagina del indice y, si no hay coincidencia, por `sourcePage` como aproximacion. El documento fuente y el `.docupodcast.json` no cambian.

## Archivos clave

- `presentation/document/DocumentIndexPanel.java`
- `presentation/document/DocumentWorkspaceView.java`
- `application/document/BuildDocumentOutlineUseCase.java`
- `application/document/DocumentOutlineProjection.java`
- `application/document/DocumentOutlineEntry.java`
- `infrastructure/document/PdfBookmarkOutlineHintProvider.java`
- `presentation/sidedock/SideDockModuleId.java`
- `presentation/sidedock/SideDockStatePolicy.java`
- `presentation/sidedock/WorkspaceSideDock.java`
- `css/document-reader.css`

## Guardarrailes

- `BuildDocumentOutlineUseCaseTest`
- `PdfBookmarkOutlineHintProviderTest`
- `DocumentIndexHf13SourceTest`
- `DocIndexUxHf1SourceTest`
- Refuerzo de `DocumentWorkspaceSideDockSourceTest`

## Fuera de alcance

- No modifica playback.
- No toca motores ni descarga de Voz IA avanzada.
- No cambia exportaciones.
- No implementa busqueda textual completa.
- No hace refactor transversal general.
- No implementa OCR ni reconstruccion LaTeX.
