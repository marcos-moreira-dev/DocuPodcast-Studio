# Tanda 99 - Motor PDF embebido completo y transversal

## Que se implemento
- Se agrego Apache PDFBox 3.0.7 como backend visual embebido para PDF.
- Se agrego `jbig2-imageio` 3.0.5 para mejorar compatibilidad con PDFs escaneados que usen JBIG2.
- Se creo el contrato transversal en `application.document`: motor, requests/resultados de render, errores controlados, metadata de documento/pagina y modelo preparatorio para visor PDF visual.
- Se implemento `PdfBoxRenderEngine` en `infrastructure.document` con inspeccion de paginas, render de pagina completa, render de crop, fondo opaco y control de pixeles/DPI.
- `PdfDocumentImporter` usa el motor embebido para obtener `sourcePageCount` confiable y declarar disponibilidad visual sin tocar la GUI.
- `PdfSourceCropRenderer` usa PDFBox primero para crops por `sourcePage` + `bbox`; Poppler queda como fallback.
- Se agrego `VISUAL_PAGE_RENDERING` a las capacidades de fuente documental para PDF.

## Que quedo fuera
- No se reemplazo el workspace Documento por visor PDF visual.
- No se agrego OCR.
- No se agrego UI para contrasenas de PDF.
- No se implemento seleccion visual por coordenadas en la interfaz.
- No se reconstruye LaTeX ni semantica de formulas/tablas.

## Decisiones tecnicas
- El motor visual vive fuera de `presentation`; ningun componente GUI importa PDFBox.
- Los resultados de render usan `BufferedImage` para mantenerse transversales y no depender de JavaFX.
- Si el DPI solicitado supera `maxPixelCount`, el motor reduce DPI; si incluso el DPI minimo seguro excede el limite, devuelve `TOO_LARGE`.
- Los PDFs protegidos con contrasena fallan con `PASSWORD_REQUIRED` hasta que exista una UI para pedir clave.
- Poppler sigue disponible solo como fallback heredado para crops/texto.

## Archivos/sistemas tocados
- `pom.xml`
- `src/main/java/module-info.java`
- `application.document`
- `infrastructure.document`
- `DocumentSourceCapabilities`
- `BrainV1CapabilityMatrix`
- Docs de PDF y problemas tecnicos
- Tests de motor PDF, importador, crops y guardarrailes fuente

## Tests ejecutados
- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PdfBoxRenderEngineTest,PdfDocumentImporterTest,PdfSourceCropRendererTest,PdfEmbeddedRendererT99SourceTest,DocumentCapabilitiesRf1SourceTest,ArchitectureBoundaryTest,ExternalProcessRunnerRf1SourceTest" test`
- `mvn -q test`

## Proximos pasos exactos
1. Tanda 100: crear el visor PDF visual del workspace Documento, activado solo cuando `ReadableDocument.format() == PDF`.
2. En Tanda 100, consumir `BuildPdfVisualDocumentUseCase` y mantener DOCX/TXT/Markdown en el lector actual.
3. Agregar cache de paginas renderizadas para no re-renderizar cada scroll o salto de pagina.
