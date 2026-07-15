# Tanda 92 — Audio del computador efectivo en playback/export

T92 hace efectivo el audio seleccionado por el usuario como clip genérico dentro del playback/export. Parte de T91 y usa `NarrationRenderPlan` para construir cues por unidad cuando una oración/rango tiene `AUDIO_CLIP`.

## Cambios principales

- `BuildPlaybackManifestUseCase` puede construir manifest desde `NarrationRenderPlan` + proyecto.
- `PlaybackCue.unitId` se usa para reproducir cues finos sin depender solo de `segmentId`.
- `ExportPodcastWavUseCase.exportPlaybackManifest(...)` concatena cues de playback.
- `DocuPodcastShellViewModel` usa render plan al reconstruir manifest y exporta desde manifest si hay unidades.

## Regla UX/producto

`Audio del computador` no significa voz humana ni efecto específico. Es un archivo que el usuario decide asociar a una oración/rango.

## Validación

Suite normal recomendada:

```bat
scripts\02-ejecutar-tests.bat
```

La validación focal en entorno ChatGPT fue estática/compilación focal sin Maven completo.
