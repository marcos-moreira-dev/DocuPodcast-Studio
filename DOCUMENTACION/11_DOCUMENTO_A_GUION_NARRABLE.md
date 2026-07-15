# 11 — Documento a guion narrable

El documento importado no debe pasar directamente al TTS. Primero debe convertirse a guion narrable.

## Por qué

- Permite revisar el texto antes del audio.
- Permite segmentar documentos largos.
- Permite asignar voces y estilos.
- Permite asociar imágenes.
- Permite reintentar solo partes.
- Permite reproducir con resaltado.

## Flujo

```text
ReadableDocument
  → DocumentBlock[]
  → ReadingProfile
  → NarrationScriptDocument
  → NarrationSegment[]
```

## Segmentos

Un segmento puede agrupar título + párrafos o una unidad de diálogo.

Cada segmento debe tener:

```text
id
text
title/source label
sourceBlockIds
voiceProfileId opcional
characterId opcional
styleId opcional
imageBinding opcional
audioClip opcional
status
```

## Plantillas narrativas

Títulos/subtítulos pueden generar frases como:

```text
Nuevo tema: {texto}.
Ahora veremos: {texto}.
```

Esto debe ser configurable.

## Regla

El guion es el artefacto central. El documento original es fuente. El audio y storyboard dependen del guion.
