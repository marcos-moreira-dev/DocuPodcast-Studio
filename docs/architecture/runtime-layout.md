# Runtime layout

`ApplicationRuntimeLayout` resolves runtime assets from one application root.

| Path | Purpose | Git policy |
|---|---|---|
| `tools/ffmpeg/bin` | FFmpeg and FFprobe | ignored local binary |
| `tools/piper` | Piper executable/runtime | ignored local binary |
| `tools/xtts-wrapper` | tracked wrappers plus ignored `.venv` | mixed |
| `tools/image/ComfyUI` | local image engine | ignored installation |
| `models` | image and voice weights | weights ignored; manifests/licenses tracked |
| `voice-library` | application-level reference voices | tracked product samples where applicable |
| `src/main/resources/examples` | official bundled examples | tracked |

Configured relative paths resolve against the application root. Absolute paths remain supported for advanced local configuration, but source code and `pom.xml` must not contain developer-specific absolute paths.

Run `scripts/maintenance/audit-local-runtime.ps1` to compare the installation with `runtime/local-runtime-manifest.json`.

Large image weights can appear at several ComfyUI paths through NTFS hardlinks. Do not replace or delete those aliases independently without checking their hardlink set.

