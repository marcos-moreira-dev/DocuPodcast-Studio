# Local engines

DocuPodcast Studio invokes local engines through process adapters.

## Video

FFmpeg and FFprobe are expected under `tools/ffmpeg/bin`. Video preflight validates executables and required codecs before export.

## Voice

- Piper provides the simple local voice path.
- XTTS uses tracked wrapper scripts in `tools/xtts-wrapper` and an ignored Python environment.
- Voice weights live under `models/tts`.
- User/reference voice samples are managed through the application voice library.

## Image

ComfyUI is expected under `tools/image/ComfyUI`. Model paths are configured through `tools/image/extra_model_paths.yaml` and application settings. Intermediate outputs and engine input/output folders are disposable staging data.

## Verification

```powershell
.\scripts\maintenance\audit-local-runtime.ps1
.\scripts\23-preflight-arranque-motores.bat
```

Preparation scripts may download third-party artifacts, but normal generation and rendering use local processes. License and redistribution checks remain mandatory before packaging external binaries or model weights.

