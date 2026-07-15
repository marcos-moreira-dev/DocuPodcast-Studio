# Tanda 96 - Correccion layout estudio, lienzo y exportacion PNG

## Que se implemento

- El lector documental mueve el playbar al rail izquierdo cuando el SideDock derecho de estudio esta abierto, igual que teatro.
- `DocuPodcastShellView` recorta el host del workspace para impedir que un dock o sombra pinte sobre la cinta superior.
- `DocumentStudySideDock` mantiene la carcasa derecha comun y envuelve el modulo Problema en `ScrollPane` vertical.
- La vista previa del nuevo problema en el panel derecho ya no antepone IDs de bloque al texto seleccionado.
- `BuildDocumentOutlineUseCase` rechaza inferencias pobres en PDFs grandes cuando estan dominadas por bibliografia/referencias o cubren muy poco del documento; en esos casos cae a paginas PDF.
- `TechnicalProblemDialog` usa `FlowPane` para controles responsivos del lienzo y evita botones comprimidos en una sola linea rigida.
- El modo `Pantalla completa` ahora oculta titulo, enunciado, notas y herramientas secundarias, maximiza la ventana y se restaura con `Escape`.
- El lienzo se expande para cubrir el viewport cuando hace falta, ademas de crecer hacia abajo/derecha por borde o scroll.
- El flujo de creacion separa `Guardar` de `Guardar + exportar PNG...`; la segunda accion abre `FileChooser` y no cierra el dialogo si el usuario cancela.
- El PNG externo se exporta despues de guardar el problema como asset del proyecto, sin cambiar el esquema `.docupodcast.json`.

## Que quedo fuera

- OCR local.
- Reconstruccion LaTeX editable.
- Galeria/detalle visual de problemas guardados.
- Persistir imagenes del lienzo como objetos editables independientes.

## Decisiones tecnicas

- El playbar en rail izquierdo ya no depende de `THEATRE_SCRIPT`; depende de que el dock derecho este expandido.
- El scroll del panel Problema vive en `DocumentStudySideDock`, no en `WorkspaceSideDock`, para conservar header y rail.
- La exportacion externa inicial usa una ruta opcional en `TechnicalProblemResult`; la persistencia del proyecto sigue usando `STUDY_SOLUTION_IMAGE`.
- En edicion de problemas guardados no se agrego `Guardar + exportar PNG...`; la exportacion sigue en la accion del panel para evitar reemplazos accidentales por lienzo vacio.
- La validacion de indice PDF prefiere paginas si las secciones inferidas son pocas para un PDF grande o parecen ruido bibliografico.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildDocumentOutlineUseCase.java`
- `src/main/resources/css/document/study-problem.css`
- Tests fuente y unitarios de documento/indice.
- Docs de estudio documental e indice PDF.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=BuildDocumentOutlineUseCaseTest,StudyDocumentUxT95SourceTest,StudyDocumentUxT96SourceTest,DocumentWorkspaceSideDockSourceTest,DocumentWorkspaceViewSourceTest" test`
- `mvn -q "-Dtest=DocumentOperationalWorkspaceSourceTest,TheatrePlaybarLayoutSourceTest,StudyDocumentUxT96SourceTest" test`
- `mvn -q "-Dtest=StudyDocumentUxT96SourceTest,StudyDocumentUxT95SourceTest,BuildDocumentOutlineUseCaseTest" test`
- `mvn -q test`

## Proximos pasos exactos

1. Tanda 97: vista detalle/galeria de problemas con miniaturas de crops fuente y solucion PNG.
2. Agregar preview ampliada de solucion guardada sin abrir el editor completo.
3. Evaluar busqueda visual por miniaturas cuando haya muchos problemas guardados.
