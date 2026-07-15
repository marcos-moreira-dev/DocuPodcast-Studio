# T123-CAT01 — Markdown como documento, no guion

## Objetivo

Cerrar la vía heredada en la que Markdown podía presentarse como “guion” importable/exportable. Desde esta tanda, Markdown pertenece al mismo contrato de entrada que Word/DOCX, PDF con texto nativo y TXT: se abre como documento fuente solo lectura y luego DocuPodcast prepara internamente la lectura.

## Decisión de producto

DocuPodcast Studio no ofrece al usuario “subir guion”, “importar guion Markdown” ni “exportar guion Markdown”. El usuario abre documentos y escucha documentos.

Regla final:

```text
Markdown = documento fuente.
Markdown no es guion.
Markdown no declara contrato especial importable por la UI.
```

## Cambios aplicados

### UI / Shell

- Se retiraron los handlers visibles heredados:
  - `handleImportScriptMarkdown()`
  - `handleExportScriptMarkdown()`
- `DocuPodcastShellViewModel` ya no expone:
  - `importNarrationScriptMarkdown(Path)`
  - `exportNarrationScriptMarkdown(Path)`
- El flujo normal de Markdown queda exclusivamente en `Abrir documento` mediante `DocumentSourceImportService` y `MarkdownDocumentImporter`.

### Servicios de aplicación

- `ScriptApplicationServices` ya no expone importador Markdown de narración.
- `ExportApplicationServices` ya no expone exportador Markdown de narración.
- `ApplicationServicesFactory` ya no instancia import/export Markdown de guion.
- Se retiraron del build principal los use cases heredados de contrato Markdown especial:
  - `NarrationScriptMarkdownParser`
  - `ImportNarrationScriptMarkdownUseCase`
  - `ExportNarrationScriptMarkdownUseCase`

### Capacidades y toolbar legacy

- `WorkspaceCapability.IMPORT_SCRIPT_MARKDOWN` fue eliminado.
- `MainToolbarView` y `WorkspaceCapabilityCommandMapper` ya no mencionan importación Markdown de narración.

### Recursos IA

- El catálogo oficial de recursos IA ya no registra `docupodcast-script-v1`.
- Todos los recursos IA pasan a ser guías, plantillas o ejemplos documentales no importables como contrato especial.
- Se reemplazaron recursos heredados de guion por recursos orientados a documento:
  - `01_prompt_preparar_documento_lectura.md`
  - `02_checklist_documento_lectura.md`
  - `03_plantilla_notas_documento.md`
  - ejemplo Markdown de documento académico mínimo.
- `ClasspathAiResourceExporter` ahora declara explícitamente que Markdown se trata como documento fuente normal.

### Guía integrada

- Se retiraron los topics heredados:
  - `narration-script.md`
  - `ai-markdown-resources.md`
- `ClasspathGuideCatalog` ya no registra Guion ni IA/Markdown como contrato de guion.
- Los topics conservados hablan de documento, lectura preparada, audio, voces y panel visual.

### Exportación y readiness

- `ExportableArtifactKind.NARRATION_MARKDOWN` fue retirado.
- `InspectExportReadinessUseCase` ya no ofrece salida de Markdown de guion.
- El paquete auditable exporta `lectura_preparada.md` como evidencia de lectura preparada, no `guion_narrable.md`.
- Los textos de exportación se ajustan a “lectura preparada”, “panel visual” y “fragmentos”.

## Guardarraíles

- Markdown debe aparecer como documento compatible en el selector de fuente.
- No debe aparecer `IMPORT_SCRIPT_MARKDOWN`.
- No debe aparecer `docupodcast-script-v1` en recursos oficiales de `src/main` ni `src/main/resources`.
- No debe aparecer import/export Markdown de narración en el shell.
- El paquete de exportación no debe generar `guion_narrable.md`.

## Validación focal realizada

- Compilación focal de `domain`, `application`, `infrastructure` y bootstrap no JavaFX: OK.
- Búsqueda en `src/main/java` y `src/main/resources`: sin `docupodcast-script-v1`, `guion_narrable`, `IMPORT_SCRIPT_MARKDOWN`, import/export Markdown de narración.
- Tests fuente actualizados para defender Markdown como documento.

## Próxima tanda recomendada

T124 — Cuarentena legacy workspaces/tests.
