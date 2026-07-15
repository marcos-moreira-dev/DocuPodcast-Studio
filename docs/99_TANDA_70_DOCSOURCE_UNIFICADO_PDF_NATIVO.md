# Tanda 70 — DocumentSource unificado y PDF con texto nativo

## Base

Aplicable sobre T69B verde.

## Objetivo

Cerrar el ingreso documental del cerebro para V1:

```text
Abrir documento
→ detectar tipo
→ importar DOCX / PDF nativo / Markdown / TXT
→ crear Documento narrable de solo lectura
```

## Regla crítica

PDF solo entra si contiene texto nativo extraíble. Si el PDF es escaneado o imagen, DocuPodcast muestra aviso y no intenta OCR.

## Cambios principales

- Agrega `DocumentSourceType`, `DocumentSourceDescriptor`, `DocumentSourceImportService`.
- Agrega `SourceDocumentRequirementException`.
- Agrega `PdfDocumentImporter`.
- Cablea `PdfDocumentImporter` en `InfrastructureServicesFactory`.
- Actualiza el selector principal para incluir `*.pdf` bajo “PDF con texto nativo”.
- Captura rechazo de PDF no compatible con `UserNotification.warning`.
- Actualiza Welcome y documentación raíz.

## Tests

- `PdfDocumentImporterTest`.
- `DocumentSourceImportServiceTest`.
- `PdfNativeTextIntakeSourceTest`.
- Ajuste de `EditableSettingsAndFrontHonestySourceTest`.

## Validación ChatGPT

Se validó integridad de ZIP, compilación aislada de dominio/application/infrastructure documental y tests nuevos/modificados con stubs JUnit. Maven completo no se ejecutó porque el entorno no dispone de `mvn`.
