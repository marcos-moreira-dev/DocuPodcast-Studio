# Tanda 105 - Modal de problema tecnico definitivo

## Implementado

- El modal mantiene titulo y encabezado `Problema tecnico`.
- El enunciado sigue como panel continuo con texto y fuentes visuales reales.
- Se agrego `Transferir todas al lienzo` cuando el enunciado contiene capturas/crops/imagenes transferibles.
- Cada imagen individual conserva `Transferir imagen al lienzo`.
- El modo de foco del modal conserva `Pantalla completa` / `Salir de pantalla completa`, oculta enunciado/titulo/notas y permite salir con `Escape`.
- Los controles del lienzo siguen agrupados en `FlowPane` para evitar botones comprimidos o texto vertical.

## Quedo fuera

- Exportacion PNG premium con recorte configurable y diagnostico de escala queda para T107.
- OCR, capa textual PDF, busqueda y resaltado de lectura siguen fuera.

## Decisiones tecnicas

- No se cambio `.docupodcast.json`.
- Las capturas PDF siguen llegando como `StudyProblemSourceDraft`.
- Las imagenes transferidas siguen fusionandose en el PNG final de solucion; no se guardan como objetos editables.

## Archivos/sistemas tocados

- `TechnicalProblemDialog`
- `study-problem.css`
- Documentacion de estudio documental y roadmap post T100.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=StudyDocumentUxT95SourceTest,StudyDocumentUxT96SourceTest,StudyDocumentUxT97SourceTest,StudyDocumentUxT98SourceTest,PdfRegionTechnicalProblemT103T104SourceTest" test`
- `mvn -q test`

## Proximos pasos

- T106: extraer y reforzar el lienzo seguro de alta fidelidad como componente separado por tiles.
