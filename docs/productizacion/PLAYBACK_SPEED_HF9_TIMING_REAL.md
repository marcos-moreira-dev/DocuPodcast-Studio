# PLAYBACK-SPEED-HF9 — transición inmediata a 1.5x/1.75x

## Estado

Implementada sobre `DOC-INDEX-PLAYBACK-HF1` verde.

## Problema

El audio por chunk sí se aceleraba a `1.5x` y `1.75x`, pero la transición podía sentirse lenta: cuando un WAV acelerado terminaba, el siguiente chunk podía esperar un watchdog con margen fijo, dando la sensación de que todavía se respetaba la duración de `1x`.

## Causa técnica

La reproducción tenía varias señales de avance:

- callback real de fin natural del reproductor;
- cola secuencial;
- deadline/watchdog;
- reloj de cursor.

El callback real era la mejor señal, pero si no llegaba o no coincidía exactamente con el path esperado, el fallback usaba márgenes conservadores dispersos (`1.25`, `0.75`, `750L`, `300L`, etc.). En chunks cortos esa espera se percibía como silencio artificial.

## Cambios

### `PlaybackTimingPolicy`

Se agrega `PlaybackTimingPolicy` como política central para nombrar y compartir reglas de tiempo:

- `watchdogSafetyMarginSeconds`
- `deadlineSafetyMarginSeconds`
- `minimumWatchdogDelayMillis`
- `shortProbeDelayMillis`
- `pollIntervalMillis`
- `completionGraceSeconds`
- `transitionGuardMillis`

La política calcula delays usando `duration / playbackRate`, por lo que `1.75x` también acorta el tiempo de fallback.

### Cola secuencial

`PlaybackSequentialQueueDriver` ahora usa `PlaybackTimingPolicy` en vez de números mágicos. El callback natural del reproductor sigue siendo la señal preferida; el watchdog queda como red breve si el callback no llega.

### Deadline secundario

`PlaybackCueDeadlineSequencer` usa la misma política para deadline, polling y gracia de final de cue.

### Coincidencia robusta de fin de WAV

`DocuPodcastShellViewModel.matchesCompletedAudioFile(...)` conserva la comparación absoluta exacta, pero acepta también una coincidencia robusta por nombre de archivo cuando el reproductor reporta el WAV normalizado de forma equivalente. Esto reduce la probabilidad de ignorar un fin natural válido y caer al watchdog.

## Resultado esperado

A `1.5x` y `1.75x`, el siguiente chunk debe iniciar cuando termina el audio acelerado. Si el callback real falla, el fallback ya no debe sentirse como espera de duración `1x`.

## Tests

Agregados:

- `PlaybackTimingPolicyTest`
- `PlaybackSpeedHf9TimingPolicySourceTest`

Actualizado:

- `PlaybackCore2DeadlineSequencerSourceTest`

## Validación recomendada

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Abrir un documento con varias frases cortas.
3. Generar chunks.
4. Reproducir a `1x`, `1.5x` y `1.75x`.
5. Confirmar que a `1.75x` no queda silencio largo entre frases.
