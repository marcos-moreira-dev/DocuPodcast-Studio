# 17 — Markdown y recursos IA

Markdown es puente humano/IA, no persistencia completa.

## Contratos propuestos

```text
docupodcast-script-v1
docupodcast-storyboard-v1
docupodcast-voice-library-v1
docupodcast-project-v1 futuro
```

## Frontmatter

```yaml
---
docupodcast_type: "script"
contract: "docupodcast-script-v1"
title: "Mi obra teatral"
language: "es"
importable: false
sample_kind: "full-example"
---
```

## Recursos IA

La app debe exportar:

```text
00_indice_recursos_ia.md
gramáticas
plantillas
prompts
ejemplos académicos
ejemplos teatrales
ejemplos storyboard
referencias de voces autorizadas
```

## Regla

Plantillas con placeholders no deben marcarse importables. Un ejemplo oficial solo podra marcarse importable cuando exista parser real, workspace editable, guardado y test de roundtrip.

## Flujo IA

```text
Word o texto fuente
  + prompt DocuPodcast
  → Markdown docupodcast-script-v1
  → revisar como referencia o implementar parser primero
  → importar solo cuando exista cadena real
  → revisar en ScriptWorkspace
  → guardar .docupodcast
```
