# 12 — Guion, segmentos, selección y rangos

El workspace de Guion debe tratar el guion como expediente estructurado, no como canvas.

## Selección

Debe existir un modelo único:

```text
ScriptSelection
  kind
  id
  ownerId
  range opcional
```

Kinds esperados:

```text
DOCUMENT
SCRIPT_SECTION
SEGMENT
TEXT_RANGE
CHARACTER
VOICE_PROFILE
PERFORMANCE_STYLE
PERFORMANCE_SPAN
STORYBOARD_BINDING
AUDIO_CLIP
AUDIO_JOB
NOTE
NONE
```

## Rango de performance

El usuario debe poder seleccionar texto con mouse y asignar:

- voz;
- personaje;
- estilo;
- pausa;
- imagen;
- regenerar selección.

MVP: aplicar por segmento completo. Luego aplicar por rango interno.

## Modelo

```text
PerformanceSpan
  id
  segmentId
  startOffset
  endOffset
  voiceProfileId
  characterId
  styleId
  intensity
```

## UI

El centro debe mostrar segmentos con chips:

```text
[Voz: Narrador] [Estilo: neutro] [Imagen: salon.png] [Audio: generado]
```

Durante playback, el segmento activo se resalta.
