# Hotfix T116C - Captura PDF y lienzo estable

## Implementado

- La seleccion rectangular del visor PDF ahora instala filtros de evento en el overlay y en el frame de pagina renderizada. Esto evita depender solo del `ImageView` y permite interceptar el drag antes de que el scroll lo consuma.
- La conversion de coordenadas usa los bounds visibles reales de la pagina renderizada para generar `PdfViewportSelection`.
- La barra superior del enunciado del modal queda alineada a la izquierda, con padding izquierdo amplio, y el boton se llama `Cargar imagen externa` sin puntos suspensivos visibles.
- El crecimiento del lienzo por scroll ahora usa incremento de 25% en ancho/alto, con guard reentrante y debounce para reducir congelamientos.
- El crecimiento no usa snapshots gigantes; delega en la superficie por tiles existente.

## Fuera de alcance

- OCR nuevo.
- Cambios de esquema `.docupodcast.json`.
- Cambios en Domain Model Studio/UENS.

## Decisiones

- La captura PDF se mantiene como gesto de producto: `capturas PDF`, sin exponer `bbox` ni detalles tecnicos al usuario.
- Si un eje del lienzo ya no puede crecer por limite interno, el otro eje todavia puede crecer.

## Archivos tocados

- `PdfVisualDocumentView`
- `TechnicalProblemDialog`
- `StudyProblemCanvasSurface`
- `study-problem.css`
- Tests fuente de PDF/modal

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,PdfVisualFlowT113SourceTest,StudyProblemWorkflowTest" test`
- `mvn -q "-Dtest=TechnicalProblemHotfixT116BSourceTest,TechnicalProblemT116C117SourceTest,PdfVisualFlowT113SourceTest,StudyProblemWorkflowTest,PdfEmbeddedRendererT99SourceTest,StudyDocumentUxT97SourceTest,StudyDocumentUxT98SourceTest" test`
- `mvn -q test`

## Siguiente tarea exacta

- Validar manualmente el drag rectangular sobre PDF en 78%, 100% y zoom alto; si todavia falla en la app, instrumentar logs del frame/overlay para ver si el evento llega y si `displayedImageBounds()` devuelve bounds validos.
