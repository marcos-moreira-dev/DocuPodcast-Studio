# T92 — Audio del computador efectivo en playback/export

## Objetivo

T92 conecta el contrato de unidades narrativas introducido en T91 con playback y exportación. Hasta T91, una capa `HUMAN_AUDIO` podía existir como dato de proyecto, pero el playback seguía usando principalmente WAVs generados por segmento. Desde T92, una unidad de render que use `AUDIO_CLIP` puede convertirse en cue real de playback y fuente de exportación WAV.

## Alcance

- `BuildPlaybackManifestUseCase` agrega un overload que recibe `NarrationRenderPlan` y `DocuPodcastProject`.
- Las unidades `NarrationRenderUnit` con `sourceKind = AUDIO_CLIP` resuelven su `audioAssetId` contra el catálogo de assets del proyecto.
- El cue resultante usa `unitId`, `audioClipId` y la ruta relativa del asset `AUDIO_CLIP`.
- `ExportPodcastWavUseCase` agrega `exportPlaybackManifest(...)` para concatenar las fuentes WAV descritas por el manifest.
- `DocuPodcastShellViewModel` reconstruye el manifest usando el render plan cuando hay proyecto activo.
- Al exportar WAV, si el manifest tiene cues por unidad, se exporta desde el manifest en vez de ignorar los clips asignados.

## Regla de producto

`Audio del computador` es un clip genérico elegido por el usuario. El programa no clasifica si el archivo contiene una persona hablando, pájaros, música, ambiente, ruido, una campana o cualquier otro sonido. Solo conserva el asset y lo asocia a una unidad narrativa.

## Compatibilidad

T92 no elimina el camino viejo por segmento:

- Los segmentos sin `AUDIO_CLIP` siguen usando el WAV generado por el job TTS, si existe.
- Los segmentos con unidades de audio externo usan esas unidades como material efectivo.
- `PlaybackCue` conserva compatibilidad segment-level, pero `unitId` ya es el identificador fino para cues por oración/rango.

## Limitación honesta

T92 no resuelve todavía una mezcla perfecta de TTS parcial + audio externo dentro del mismo segmento. Esa composición completa queda para el avance de `AudioUnit`/render por unidad más profundo. Esta tanda hace que el clip asignado deje de ser solo metadata y pueda entrar al manifest/export.

## Validación

Tests agregados/modificados:

- `BuildPlaybackManifestUseCaseTest.userAudioClipUnitsBecomeEffectivePlaybackCues`
- `ExportPodcastWavUseCaseTest.exportsPlaybackManifestWithUserAudioClipCues`
- `AudioClipPlaybackExportT92SourceTest`

## Siguiente paso

T93 — `TextAnchor` mínimo + migración controlada.
