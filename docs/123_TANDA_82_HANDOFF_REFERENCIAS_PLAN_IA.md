# Tanda 82 — Handoff de continuidad, referencias visuales y plan IA real

## Objetivo

Consolidar en el repositorio todas las notas, capturas y decisiones de diseño tomadas durante las tandas recientes para que el proyecto pueda continuar en otra ventana de chat sin perder contexto.

Esta tanda no cierra un release candidate final. El usuario aclaró que todavía falta bastante, especialmente la integración real de motores IA para producir voz y transcribir audio.

## Cambios realizados

- Se agrega carpeta de referencias visuales con capturas usadas en la planificación.
- Se crea índice visual con explicación de cada captura.
- Se consolida un documento de notas de conversación T81/T82.
- Se crea un plan exacto para integrar TTS/STT real.
- Se crea un handoff de continuidad para próximas sesiones.
- Se crea roadmap post T82 hacia IA real y RC.
- Se actualizan README, AI_HANDOFF, VALIDATION y registro de tandas.

## Archivos nuevos principales

```text
docs/referencias/capturas-chat/INDICE_REFERENCIAS_VISUALES.md
docs/productizacion/NOTAS_CONVERSACION_T81_T82.md
docs/productizacion/PLAN_EXACTO_IA_TTS_STT_REAL.md
docs/productizacion/HANDOFF_CONTINUIDAD_T82.md
docs/productizacion/ROADMAP_POST_T82_IA_REAL_Y_RC.md
```

## Capturas preservadas

Las capturas quedan en:

```text
docs/referencias/capturas-chat/app-docupodcast/
docs/referencias/capturas-chat/referencias-externas/
docs/referencias/capturas-chat/retroalimentacion-t81/
```

## Estado del producto

T81 dejó una interfaz más cercana a lector narrado:

- Documento como superficie principal.
- Inspector izquierdo contextual.
- Rail derecho visual.
- Toolbar por iconos y grupos.
- Barra flotante de lectura.
- Vistas avanzadas degradadas.

Pero todavía falta completar la cadena real de IA:

```text
motor TTS real → modelo/voz → generación WAV → reproducción
motor STT real → modelo → transcripción → persistencia
FFmpeg real → extracción audio/video y video simple
```

