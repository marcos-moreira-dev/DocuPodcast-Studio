# Tanda 29 — Playback sincronizado transversal

## Objetivo

Convertir el playback en un contrato transversal compartido por Guion, Audio y Storyboard, evitando que cada workspace interprete el cursor, el manifest y el estado visual de forma distinta.

## Cambios principales

```text
PlaybackSegmentSync
PlaybackSyncState
DocuPodcastShellViewModel.playbackSyncState()
DocuPodcastShellViewModel.playFromSegment(String segmentId)
ScriptWorkspaceView con chip Playback por segmento
AudioWorkspaceView con resumen de sincronización transversal
StoryboardWorkspaceView con reproducción directa desde escena
```

## Comportamiento

El estado de playback ahora concentra:

```text
segmento activo
posición actual
estado reproduciendo/pausado/detenido
cuántos segmentos tienen audio
cuántos segmentos tienen storyboard
labels de cues compartidos
labels de segmentos sincronizados
```

Cuando el playback avanza automáticamente de un cue al siguiente, el `selectedScriptSegmentId` también cambia. Esto mantiene alineados Guion y Storyboard con el cursor real.

## Validación

Se agregan/refuerzan tests fuente y unitarios:

```text
PlaybackSyncStateTest
PlaybackCrossWorkspaceSourceTest
AudioPlaybackManifestUiSourceTest
ScriptWorkspaceSourceTest
StoryboardWorkspaceSourceTest
```

## Limitaciones

La sincronización depende de que exista un `PlaybackManifest`, normalmente generado a partir de audio persistido por segmentos. La tanda no implementa todavía concatenación final real del podcast; eso queda para Tanda 34.
