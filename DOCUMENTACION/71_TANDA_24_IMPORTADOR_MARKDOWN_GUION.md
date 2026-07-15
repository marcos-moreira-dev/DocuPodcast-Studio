# Tanda 24 — Importador Markdown de guion narrable

## Objetivo

Convertir `docupodcast-script-v1` en una cadena real de producto: Markdown compatible → parser → guion editable → workspace → guardado/exportación.

## Cambios principales

```text
NarrationScriptMarkdownParser
ImportNarrationScriptMarkdownUseCase
ScriptApplicationServices.importNarrationScriptMarkdown
WorkspaceCapability.IMPORT_SCRIPT_MARKDOWN
Menú Guion → Importar guion Markdown…
Toolbar contextual → Importar guion / Importar Markdown
```

## Contrato soportado

El Markdown debe declarar frontmatter:

```yaml
docupodcast_type: "script"
contract: "docupodcast-script-v1"
title: "..."
language: "es"
importable: true
```

Los segmentos se importan desde encabezados:

```markdown
### SEG-001 — Título

personaje: CHR-NARRATOR
voz: VOC-NARRATOR
estilo: STY-NEUTRAL

Texto:
> Texto narrable.
```

También se soporta el formato exportado por la app con atributos en lista y backticks.

## Recursos IA

Los ejemplos oficiales completos pasan a `importable: true` porque existe parser real. Las plantillas con placeholders siguen en `importable: false`.

## Validación

Se agregan pruebas de parser, import use case e importabilidad real de recursos oficiales. En este entorno no se ejecutó Maven porque `mvn` no está instalado; se validó con `javac` y prueba manual del parser sobre los ejemplos oficiales.
