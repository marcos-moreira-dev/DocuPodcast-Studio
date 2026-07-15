# Tanda 57 — Streaming/prebuffer robusto

## Objetivo

Consolidar la reproducción por chunks para documentos largos: el usuario debe poder pulsar **Escuchar documento**, esperar solo un buffer inicial razonable y continuar la lectura mientras la fábrica de audio prepara fragmentos en segundo plano.

## Contrato de producto

- No esperar a que un documento largo se renderice completo antes de escuchar.
- Usar 5 fragmentos como buffer inicial por defecto.
- Mantener hasta 10 fragmentos adelantados como lookahead recomendado.
- Si la lectura alcanza un fragmento aún no generado, pausar de forma clara, preparar el siguiente chunk y continuar automáticamente.
- Mantener todo lo técnico detrás de la pantalla Documento.
- El usuario ve un estado de buffer simple; no necesita entender jobs, manifests ni carpetas internas.

## Cambios principales

- Se agrega `domain.playback.StreamingPlaybackWindow` para calcular estado de buffer, inicio, lookahead y espera por siguiente fragmento.
- `PlaybackBufferPolicy` ahora expone métodos de política robusta:
  - `initialTargetFor`
  - `lookaheadTargetAfter`
  - `isSegmentReady`
  - `shouldWaitForNextChunk`
  - `waitingLabel`
- `DocuPodcastShellViewModel` expone `streamingBufferStatusProperty()` y refresca el estado visible con `refreshStreamingBufferStatus()`.
- `DocumentWorkspaceView` muestra el estado del buffer dentro del flujo Word → escuchar.
- `SettingsDialog` documenta que Tanda 57 consolida continuidad automática y estado visible de buffer.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Tests agregados:

```text
StreamingPlaybackWindowTest
StreamingPlaybackRobustnessSourceTest
```

## Alcance no incluido

- No implementa todavía optimización real de memoria por motor.
- No conecta preferencias persistentes de buffer.
- No altera FFmpeg, video ni motores reales.
- No cambia el contrato de asignaciones de voz/audio/imagen.
