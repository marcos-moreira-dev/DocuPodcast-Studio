# Markdown y recursos IA

Markdown no reemplaza a `.docupodcast.json`. Markdown es puente humano/IA.

## Contratos propuestos

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

## Recursos IA exportables

La app debe exportar:

- gramáticas;
- plantillas;
- prompts;
- ejemplos académicos;
- ejemplos teatrales;
- ejemplos de storyboard;
- guía de voces autorizadas.

## Regla de importabilidad

Una plantilla con placeholders no debe marcarse `importable: false`.

Un ejemplo oficial solo puede marcarse importable cuando exista parser real, workspace editable, guardado y test de roundtrip.
