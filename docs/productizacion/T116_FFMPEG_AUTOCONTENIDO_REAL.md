# T116 — FFmpeg autocontenido real

## Objetivo

Endurecer el contrato de FFmpeg para que la distribución final no dependa de PATH ni instalaciones manuales del usuario. FFmpeg y FFprobe deben viajar como herramientas locales verificables dentro de `tools/ffmpeg/bin/`.

## Cambios principales

- `BuildRuntimeBundleManifestUseCase` ahora trata `ffprobe.exe` como artefacto obligatorio para producto portable final.
- `scripts/29-verificar-runtime-layout.bat` valida `tools\ffmpeg\bin\ffmpeg.exe` y `tools\ffmpeg\bin\ffprobe.exe`, no solo carpetas.
- `scripts/tts/preflight-piper-ffmpeg.ps1` trata FFprobe como requerido y reporta versiones/encoders.
- Se agrega `FfmpegRuntimeProbeUseCase` para consultar `ffmpeg -version`, `ffprobe -version` y `ffmpeg -encoders` sin usar PATH.
- Se agrega `FfmpegRuntimeReport` con soporte explícito para `libx264`, `h264_nvenc`, `h264_qsv` y `h264_amf`.

## Política

- FFmpeg es obligatorio para media/video.
- FFprobe es obligatorio para validación de video final.
- CPU/libx264 es el fallback universal.
- NVENC/QSV/AMF solo deben ofrecerse si `ffmpeg -encoders` los reporta.
- Esta tanda no descarga FFmpeg ni incluye binarios; endurece el contrato y la verificación.

## Validación

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Para una validación de layout final:

```bat
scripts\29-verificar-runtime-layout.bat
```

En una RC final, `29-verificar-runtime-layout.bat` debe fallar si faltan `ffmpeg.exe` o `ffprobe.exe`.
