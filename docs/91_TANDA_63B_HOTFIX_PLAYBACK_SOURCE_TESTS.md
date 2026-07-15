# Tanda 63B — Hotfix de source tests de playback tras coordinador

## Motivo

La Tanda 63 movió parte del contrato de buffer y espera desde `DocuPodcastShellViewModel` hacia `PlaybackWorkflowCoordinator`. El comportamiento seguía representado, pero dos source tests seguían buscando la frase `playbackBufferPolicy.waitingLabel()` directamente dentro del shell.

## Corrección

Los tests de playback ahora reconocen el nuevo reparto de responsabilidades:

- `DocuPodcastShellViewModel` conserva el estado visible, `streamingBufferStatusProperty`, `tryStartBufferedPlayback`, `tryContinueAfterBufferGap` y `waitForBufferedContinuation`.
- `PlaybackWorkflowCoordinator` centraliza `waitingForBufferMessage` y la llamada a `policy.waitingLabel()`.

Esto evita forzar que el ViewModel vuelva a concentrar reglas que ya fueron extraídas al cerebro.

## Alcance

No cambia comportamiento productivo. Alinea los guardarraíles con el refactor T63.
