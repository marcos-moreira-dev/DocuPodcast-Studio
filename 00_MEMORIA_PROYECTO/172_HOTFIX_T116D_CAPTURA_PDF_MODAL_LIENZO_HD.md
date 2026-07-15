# Hotfix T116D: captura PDF real, modal alineado, lienzo estable y exportacion HD

## Implementado

- El dialogo de carga documental muestra el indicador y el microcopy en una misma fila, con el texto al lado del spinner.
- La seleccion rectangular de PDF paso a una capa global transparente sobre el visor, ligada al tamano real del host visual.
- La captura PDF detecta la pagina bajo el mouse con coordenadas de escena y usa los bounds visibles del frame renderizado para convertir la seleccion a coordenadas de imagen/PDF.
- La seccion Enunciado del modal de problema tecnico usa fondo blanco, padding interno consistente y botones alineados con el contenido.
- El boton visible de carga externa queda como `Cargar imagen externa`, sin puntos suspensivos.
- El crecimiento del lienzo por scroll ahora se coalesce con debounce y guard reentrante para evitar cascadas de eventos del scrollbar.
- La exportacion mejora la calidad de escalado de imagenes con interpolacion bilineal y permite exportacion externa premium con limite seguro mas alto.

## Fuera de alcance

- OCR nuevo, indice PDF avanzado y mejoras semanticas de temario.
- Cambio de esquema `.docupodcast.json`.
- Cambios a Domain Model Studio/UENS.
- Verificacion manual completa con Computer Use: el plugin conecto, pero no habia una ventana activa de DocuPodcast Studio disponible para inspeccion sin abrir una nueva sesion.

## Decisiones tecnicas

- La capa de captura PDF global evita depender del `ImageView` o de overlays por pagina que fallaban con el layout virtualizado/zoom.
- La conversion de seleccion prioriza los bounds del frame de pagina renderizada y usa el `ImageView` solo como dato de imagen.
- El crecimiento del lienzo se difiere con `PauseTransition` para no crecer dentro del mismo ciclo de eventos del scrollbar.
- La exportacion HD no hace snapshot gigante de JavaFX; mantiene composicion propia y mejora el muestreo al escalar.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/DocumentImportProgressDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasExportOptions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `src/main/resources/css/document/pdf-visual-viewer.css`
- `src/main/resources/css/document/study-problem.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT116C117SourceTest.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q -Dtest="PdfRegionTechnicalProblemT103T104SourceTest,PdfVisualFlowT113SourceTest,TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,StudyProblemExportT107SourceTest" test`
- `mvn -q test`

## Proximos pasos

1. Abrir DocuPodcast Studio y validar manualmente captura PDF con zoom 78%, 100% y alto.
2. Si aun falla en una maquina real, instrumentar visualmente la capa global para mostrar bounds de pagina y coordenadas detectadas.
3. Continuar con la siguiente tanda de seleccion interna avanzada del lienzo y exportacion PDF de ejercicios si el hotfix queda estable.
