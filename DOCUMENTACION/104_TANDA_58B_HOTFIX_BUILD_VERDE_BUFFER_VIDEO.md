# Tanda 58B — Hotfix build verde: buffer y video

Esta tanda corrige el estado rojo posterior a Tanda 58 sin mezclar refactor ni rediseño visual.

## Correcciones

1. `DocuPodcastShellViewModel` deja de duplicar el mensaje de espera de buffer y reutiliza `PlaybackBufferPolicy.waitingLabel()`.
2. `ExportSimpleVideoPackageUseCase` alinea el lenguaje de video: la salida actual es paquete renderizable/auditable; el MP4 final real sigue pendiente de conectar render por frames con FFmpeg.
3. `StreamingPlaybackBufferSourceTest` y `SimpleVideoExportSourceTest` quedan alineados con esos contratos.

## Criterio de salida

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

La base debe volver a cero fallos antes de iniciar auditoría GUI, cambio de scaffolding o refactor grande.
