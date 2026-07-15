# Refactor del cerebro — Capas narrativas T65

## Problema

La lógica de capas narrativas estaba repartida dentro de `DocuPodcastShellViewModel`: IDs de asignación, targets placeholder, mensajes, conflictos, conversión de rangos, remoción de capa principal y proyección de rail.

Eso hacía que el shell actuara como backend de capas y dificultaba evolucionar la UI sin romper reglas de negocio.

## Solución

Se introduce `NarrativeLayerCoordinator` como coordinador de cerebro para:

- asignar capa de voz/audio/emoción/imagen/ambiente/nota;
- resolver conflictos de capa primaria;
- mapear rango documental a rango de la proyección interna de narración;
- quitar capa principal;
- localizar asignaciones existentes;
- generar presentaciones para el rail.

## Regla de producto

La operación nace en el Documento narrable. El guion/proyección interna solo sirve para rangos TTS/playback y compatibilidad técnica.

```text
Documento fuente solo lectura
→ Documento narrable
→ selección de bloque/oración
→ capa del proyecto
→ audio/storyboard/video derivados
```

## Deuda explícita

T65 todavía conserva targets placeholder:

```text
VOICE-IA-DEFAULT
AUDIO-EXTERNO-PENDIENTE
IMAGE-STORYBOARD-PENDIENTE
AUDIO-AMBIENTE-PENDIENTE
```

La siguiente fase debe convertir esas asignaciones en selección real de voz, audio, imagen o ambiente, sin prometer más que lo implementado.
