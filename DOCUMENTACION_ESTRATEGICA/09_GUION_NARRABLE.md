# Guion narrable

El guion narrable es el artefacto central. No es texto plano ni canvas: es documento estructurado.

## Objetivo

Transformar el documento fuente en segmentos narrables revisables y editables antes de generar audio.

## Modelo

```text
NarrationScriptDocument
  ├── sections
  ├── segments
  ├── characters
  ├── voices
  ├── styles
  ├── performanceSpans
  ├── storyboardBindings
  ├── audioReferences
  └── validationIssues
```

## Segmento

```text
NarrationSegment
  ├── id
  ├── title
  ├── text
  ├── sourceBlockIds
  ├── characterId
  ├── voiceProfileId
  ├── performanceStyleId
  ├── status
  └── notes
```

## Selección

Debe existir un modelo único de selección:

```text
ScriptSelection
  ├── kind
  ├── id
  ├── ownerId
  └── range opcional
```

Tipos:

- documento;
- sección;
- segmento;
- rango de texto;
- personaje;
- voz;
- estilo;
- imagen;
- audio clip;
- job.

## Edición

Primero implementar por segmento. Luego por rango interno.

MVP:

- seleccionar segmento;
- editar texto;
- asignar voz;
- asignar personaje;
- asignar estilo;
- ignorar segmento;
- regenerar segmento.

Futuro:

- seleccionar frase;
- aplicar estilo a rango;
- múltiples voces dentro de un segmento;
- alineación fina para playback.
