# Markdown y recursos IA

Markdown en DocuPodcast no reemplaza a Word ni a `.docupodcast.json`. Es puente humano/IA.

## Contratos previstos

- `docupodcast-script-v1`.
- `docupodcast-storyboard-v1`.
- `docupodcast-voice-library-v1`.
- `docupodcast-project-v1` futuro.

## Frontmatter

```yaml
---
docupodcast_type: "script"
contract: "docupodcast-script-v1"
title: "Mi obra"
language: "es"
importable: false
---
```

## Recursos IA

La app debe poder exportar:

- gramáticas;
- plantillas;
- ejemplos mínimos;
- ejemplos completos;
- prompt maestro;
- guía de voces autorizadas;
- guía de storyboard vivo.

## Regla crítica

Ningun recurso debe marcarse importable sin parser/importador real. Una plantilla con placeholders siempre debe quedar no importable.

## Word-first

La guía debe dejar claro que si las notas están en Word, el usuario debe comenzar por DOCX. Markdown es útil para IA o intercambio estructurado.
