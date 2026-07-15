# Memoria — Tanda 6 Guion narrable

Tanda 6 agrega el primer guion narrable real de DocuPodcast Studio.

## Lo importante

- `NarrationScriptDocument` es el artefacto textual estructurado posterior al Documento.
- `NarrationSegment` es la unidad futura de audio, reintento, storyboard y playback.
- `BuildNarrationScriptUseCase` convierte bloques narrables en segmentos estables.
- `ValidateNarrationScriptUseCase` detecta guion vacío, segmentos vacíos y segmentos demasiado largos.
- `NarrationScriptWorkspaceFileRepository` materializa `script/narration-script.json`.
- `ScriptWorkspaceView` muestra el guion en UI sin usar canvas.

## Estado de producto

Ya existe la cadena:

```text
Word → Documento → Reading Profile → Guion narrable
```

Todavía no existe:

```text
Guion → Audio
```

Eso queda para Tanda 7.
