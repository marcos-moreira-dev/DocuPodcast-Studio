# Tanda 119 - Multiimagen externa y fuente PDF desde carpeta

## Implementado

- `Cargar imagen externa` ahora permite seleccionar multiples imagenes.
- Se admiten `png`, `jpg`, `jpeg`, `webp` y `bmp` como entrada de UI; cada imagen se convierte a PNG temporal para integrarse como fuente visual del problema.
- Se agrego el caso de uso `CreatePdfFromImageFolderUseCase`.
- Se agrego el contrato `ImageFolderPdfBuilder` y la implementacion `PdfBoxImageFolderPdfBuilder`.
- El SideDock Problema incluye `Crear fuente PDF desde carpeta de imagenes...`.
- La operacion genera un PDF dentro de `source/generated-pdf-from-images`, con una imagen por pagina y orden natural por nombre.
- El PDF generado se abre/importa como fuente documental actual para reutilizar visor PDF, seleccion rectangular y problemas por capturas.

## Fuera de alcance

- No se implemento OCR sobre imagenes.
- No se creo editor de paginas del PDF generado.
- No se cambio `.docupodcast.json`.
- No se toco Domain Model Studio/UENS.

## Decisiones tecnicas

- El PDF se genera con PDFBox, sin `ProcessBuilder`.
- La operacion requiere proyecto guardado porque el PDF generado debe vivir dentro de la carpeta del proyecto.
- Las paginas del PDF preservan la relacion de aspecto de cada imagen sobre fondo blanco.
- El orden natural evita que `10.png` quede antes de `2.png`.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/ImageFolderPdfBuilder.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/CreatePdfFromImageFolderUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxImageFolderPdfBuilder.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test` (verde; solo advertencias PDFBox de fixtures PDF con offsets de stream corregidos por workaround)

## Proximos pasos

1. Probar una carpeta real con imagenes grandes y nombres numerados.
2. Validar que el PDF generado abra como fuente y permita capturas rectangulares.
3. Completar pruebas manuales con `jpg/png/bmp/webp`; si `webp` no esta soportado por el runtime local de `ImageIO/PDFBox`, agregar conversion JavaFX antes de crear el PDF.
