# Tanda 118 - Lienzo HD, tinta fluida y recorte de imagen

## Implementado

- Se agrego `Recortar imagen` para imagenes seleccionadas en el lienzo usando la region marcada por `Seleccionar region`.
- Se agrego `Restaurar recorte` para volver a la imagen completa cuando existe respaldo.
- El recorte crea una imagen derivada en el problema actual y no destruye la fuente visual original.
- La tinta del lienzo registra comandos vectoriales de linea/curva/borrador para exportar con mejor calidad cuando el lienzo no fue rehidratado desde una solucion raster antigua.
- El dibujo rapido se subdivide en pasos intermedios y conserva curvas cuadraticas para reducir trazos rectos al usar mouse o tableta.
- La exportacion usa el modelo de tinta cuando esta disponible, con antialiasing y composicion sobre fondo/imagenes.

## Fuera de alcance

- No se cambio `.docupodcast.json`.
- No se implemento presion de lapiz.
- No se convirtieron soluciones antiguas raster a objetos vectoriales editables.
- No se agrego OCR ni reconstruccion LaTeX.

## Decisiones tecnicas

- El preview sigue usando tiles JavaFX acotados para mantener estable la ventana.
- La exportacion HD reproduce comandos de tinta cuando el estado vectorial sigue siendo confiable; si se carga una solucion PNG antigua, el sistema conserva esa capa raster para no perder contenido.
- Los recortes de imagen se aplican a la instancia del lienzo, no a la fuente original del enunciado.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test` (verde; solo advertencias PDFBox de fixtures PDF con offsets de stream corregidos por workaround)

## Proximos pasos

1. Validar manualmente con tableta digitalizadora la fluidez de trazos.
2. Probar recorte/restauracion con imagenes de enunciado, imagenes externas y capturas PDF.
3. Si la exportacion de soluciones antiguas necesita mas calidad, planificar persistencia editable opcional de trazos e imagenes sin romper v3.
