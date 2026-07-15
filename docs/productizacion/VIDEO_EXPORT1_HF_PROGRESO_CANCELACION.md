# VIDEO-EXPORT1-HF — progreso y cancelación del MP4 final

## Objetivo

Cerrar la brecha de producto de `VIDEO-EXPORT1`: exportar MP4 final no debe parecer que la aplicación se congeló. La acción visible `Exportar video` ahora se ejecuta en segundo plano, muestra progreso específico y permite solicitar cancelación del render.

## Cambios

- `ExportFinalVideoUseCase` acepta `Consumer<VideoRenderProgress>` y `BooleanSupplier` de cancelación.
- El render de cada frame informa inicio, salida útil de FFmpeg y clip completado.
- La unión final y verificación del MP4 final se reportan como fases explícitas.
- La cancelación mata `ffmpeg.exe` y procesos hijos mediante `ProcessHandle.descendants()`.
- `ExportWorkflowCoordinator` y `DocuPodcastShellViewModel` transportan progreso/cancelación sin incorporar lógica de render.
- `DocuPodcastShellView` ejecuta la exportación final en un `Task` para no bloquear JavaFX.
- Se reutiliza `VideoRenderProgressView` como superficie dedicada de progreso/cancelación.

## Fuera de alcance

- No cambia la preparación de FFmpeg.
- No toca Voz IA avanzada ni reproducción del documento.
- No reintroduce paquetes técnicos en el flujo común.
