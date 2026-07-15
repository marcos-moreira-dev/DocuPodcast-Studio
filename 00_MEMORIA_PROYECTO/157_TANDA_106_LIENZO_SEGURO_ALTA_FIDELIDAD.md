# Tanda 106 - Lienzo seguro de alta fidelidad

## Implementado

- Se extrajo la superficie del lienzo a `StudyProblemCanvasSurface`.
- El dialogo ya no contiene la implementacion interna de tiles ni exportacion por pixeles.
- La superficie separa capas de fondo, imagenes y tinta, con la tinta por encima de las imagenes.
- Los tiles siguen acotados a `1024x1024` para evitar un `Canvas` monolitico gigante.
- La composicion de guardado recibe imagenes transferidas como `ImageView` y genera un `WritableImage` con fondo opaco, imagenes y tinta.
- Se conservaron seleccion de imagen, movimiento, resize proporcional con manijas y eliminacion.

## Quedo fuera

- Escala/exportacion premium configurable y crop util opcional quedan para T107.
- Persistencia editable de objetos de lienzo queda fuera; solo se persiste PNG compuesto.

## Decisiones tecnicas

- `TechnicalProblemDialog` queda como orquestador de UI/eventos.
- `StudyProblemCanvasSurface` contiene limites de crecimiento, tiles, snapshot y composicion.
- Se mantienen limites conservadores: 3 columnas, 12 filas y degradacion de escala si el PNG excede `MAX_EXPORT_PIXELS`.

## Archivos/sistemas tocados

- `StudyProblemCanvasSurface`
- `TechnicalProblemDialog`
- Guardarrailes fuente de estudio documental.
- Documentacion de estudio documental y roadmap post T100.

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=StudyDocumentUxT95SourceTest,StudyDocumentUxT96SourceTest,StudyDocumentUxT97SourceTest,StudyDocumentUxT98SourceTest,PdfRegionTechnicalProblemT103T104SourceTest" test`
- `mvn -q test`

## Proximos pasos

- T107: exportacion PNG premium con recorte util opcional, escala 2x/4x segura y mensajes claros cuando deba degradar calidad por limite de pixeles.
