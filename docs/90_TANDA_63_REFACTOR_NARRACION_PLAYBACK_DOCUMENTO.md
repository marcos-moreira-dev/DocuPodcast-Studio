# Tanda 63 — Refactor narración/playback desde Documento

T63 continúa el refactor del cerebro desde la raíz correcta: **Documento narrable primero**. No rediseña la UI ni intenta convertir Guion en objeto padre. Extrae decisiones de narración y playback que estaban incrustadas en `DocuPodcastShellViewModel`.

## Nuevos coordinadores

- `DocumentNarrationCoordinator`: valida si un documento puede escucharse, crea la proyección interna/avanzada de narración y resume su validación.
- `PlaybackWorkflowCoordinator`: decide cuándo iniciar playback con buffer, cuál es el cue preferido desde la selección documental y cómo continuar después de una pausa por falta de chunk.

## Regla conceptual

```text
Documento fuente solo lectura → Documento narrable → proyección interna de narración → audio/playback
```

La proyección de narración sigue existiendo para TTS, playback, Markdown compatible y diagnóstico, pero no debe presentarse como segundo objeto padre para el usuario normal.

## Resultado esperado

- `DocuPodcastShellViewModel` delega más decisiones de cerebro.
- La experiencia visible sigue centrada en Documento.
- La UI no cambia de cara todavía; el trabajo es estructural.
- El camino queda abierto para extraer `AudioWorkflowCoordinator` y `NarrativeLayerCoordinator`.
