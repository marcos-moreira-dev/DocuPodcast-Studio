# Tanda 93 - UX estudio, indice PDF y preview export

## Que se implemento

- El dialogo de problema tecnico usa controles transversales mediante `StudioFormControls` y botones de `ActionButtonFactory`.
- El dialogo ahora es redimensionable, con tamano inicial amplio y ventana maximizable por el sistema.
- Se agrego toggle dinamico `Lienzo/texto` y toggle `Panear/dibujar`.
- En modo dibujar, el canvas consume los trazos; en modo panear, el `ScrollPane` mueve el lienzo sin dibujar.
- El modulo Problema salio del SideDock izquierdo y vive en un SideDock derecho para estudio documental normal.
- La pestana Estudio de la cinta queda reducida al grupo Problemas con `PREPARE_TECHNICAL_PROBLEM`.
- El parser PDF `bbox-layout` ordena bloques por coordenadas y une fragmentos cercanos de la misma columna.
- El indice PDF cae a anclas por pagina antes de navegacion plana cuando no hay bookmarks, Contents ni secciones confiables.
- El centro de exportaciones usa controles estilizados para video documental y muestra una preview de frame hipotetico con fuente, colores y tamano activos.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Presion de lapiz en tableta digitalizadora.
- Busqueda/filtros de problemas guardados.
- Gestor avanzado de indice por capitulos cuando el PDF no expone bookmarks ni Contents legibles.

## Decisiones tecnicas

- No se cambio el formato `.docupodcast.json` v3.
- No se amplio `DocuPodcastShellViewModel`; el cambio de docks queda dentro de `DocumentWorkspaceView`.
- El SideDock derecho documental reutiliza `WorkspaceSideDock.RailPlacement.RIGHT`.
- La preview de exportacion no genera assets ni audio; solo anticipa visualmente el frame.
- El fallback por paginas se modela como `DocumentOutlineOrigin.PDF_PAGES`.

## Archivos y sistemas tocados

- `presentation/components`: nuevo `StudioFormControls` y estilos de formulario.
- `presentation/document`: dialogo de problema tecnico y layout de docks del lector.
- `presentation/ribbon`: superficie Estudio simplificada.
- `infrastructure/document`: parser PDF bbox con orden/union por coordenadas.
- `application/document`: origen y construccion de indice por paginas PDF.
- `presentation/export`: controles de video documental y preview de frame.
- `docs/productizacion`: contratos de problemas tecnicos, PDF e exportacion.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildDocumentOutlineUseCaseTest,PdfBboxLayoutParserTest,StudyDocumentUxT93SourceTest,ExportCenterPreviewT93SourceTest,StudyRibbonT93SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 94: agregar busqueda y filtros de problemas guardados por pagina, bloque fuente, texto de enunciado y texto de solucion.
2. Permitir navegar desde un problema guardado hacia el bloque/pagina fuente original.
3. Evaluar una vista de indice PDF por paginas con agrupacion visual por rangos si el documento supera cientos de paginas.
