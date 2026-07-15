# TI7 — Playback por oración/unidad

## Objetivo

TI7 completa el puente técnico iniciado por TI1, TI2 y TI3 para que el playback deje de depender exclusivamente del segmento completo y pueda trabajar con unidades reales de render/narración.

El contrato nuevo es:

```text
RenderUnitPlan
→ unidades habladas
→ PlaybackManifest con unitId
→ cueForUnit / nextCueAfterUnit / seekUnit
```

## Reglas de producto

- El usuario puede seleccionar una oración o unidad dentro del documento.
- El playback debe poder saltar a la unidad correspondiente si existe audio.
- `SPOKEN_ONLY` entra al playback si tiene audio generado o audio externo.
- `SPOKEN_WITH_VISUAL` entra al playback si tiene audio generado o audio externo.
- `VISUAL_SILENT no se reproduce como audio`.
- `OMITTED` no se reproduce.
- Las tablas, imágenes y fórmulas no narrables no deben leerse como código ni como aviso largo.

## Alcance de implementación

`BuildPlaybackManifestUseCase` ahora tiene una entrada basada en `RenderUnitPlan`. Esta ruta:

1. Lee `renderUnitPlan.audioUnits()`.
2. Resuelve unidades generadas por TTS desde `AudioJobSnapshot` usando el `RenderUnit.id`.
3. Resuelve unidades con audio externo desde `ProjectAssetCatalog`.
4. Omite visuales silenciosos y unidades omitidas.
5. Genera cues con `unitId` estable para navegación fina.

`SeekPlaybackUseCase` agrega `seekUnit(...)` para que la UI pueda buscar una unidad concreta manteniendo compatibilidad con `PlaybackCursor.segmentId`.

## Lo que no hace todavía

- No cambia todavía el cursor de dominio para guardar `unitId` como campo principal.
- No rediseña la playbar.
- No hace scrub visual por palabra.
- No mezcla audio ni compone clips complejos.

Eso queda para un refactor posterior si la experiencia lo exige.

## Relación con bloques visuales fuente

Los bloques fuente no narrables siguen esta regla:

```text
imagen / tabla / fórmula fuente
→ no se narra por defecto
→ no entra al playback como audio
→ solo entra al video si tiene visual asignado
```

