# Tanda 88C — Limpieza de alcance de motores y GUI

T88C corrige el alcance visible antes de continuar con T89.

## Criterio aplicado

- DocuPodcast lee y narra documentos; no construye documentos desde audio.
- Coqui/XTTS, Piper y FFmpeg son los motores visibles de producto.
- Whisper/STT queda fuera del producto visible.
- `Audio del computador` es un clip genérico asociado a una selección.
- No se muestran opciones no implementadas como si fueran funciones avanzadas.

## Validación esperada

```bat
scripts\02-ejecutar-tests.bat
```

Tests focales:

- `SpeechToTextUiSourceTest`
- `ToolbarRecordingPlaybackSourceTest`
- `SettingsDialogSourceTest`
- `EngineSetupCatalogTest`
- `InspectAiEnginesPreflightUseCaseTest`
- `AiEngineShortlistSourceTest`
