# Local engines

DocuPodcast Studio invokes local engines through process adapters.

## Video

FFmpeg and FFprobe are expected under `tools/ffmpeg/bin`. Video preflight validates executables and required codecs before export.

## Voice

- Piper provides the simple local voice path.
- XTTS uses tracked wrapper scripts in `tools/xtts-wrapper` and an ignored Python environment.
- XTTS uses `synthesize_xtts_worker.py` for one command per chunk. The model
  may remain resident only while its atomic scheduler lease remains active.
- Voice weights live under `models/tts`.
- User/reference voice samples are managed through the application voice library.

## Image

ComfyUI is expected under `tools/image/ComfyUI`. Model paths are configured through `tools/image/extra_model_paths.yaml` and application settings. Intermediate outputs and engine input/output folders are disposable staging data.

The visual post-processing chain is provider-neutral:

1. the selected image engine renders or an external image is imported;
2. Real-ESRGAN produces the requested delivery resolution;
3. when enabled, `image-refinement` uses SD 1.5 and ControlNet Tile on the
   upscaled image with `denoise < 1`;
4. the result is decoded, dimension-checked and atomically published.

ControlNet Tile is managed at
`tools/image/ComfyUI/models/controlnet/control_v11f1e_sd15_tile.pth`.
The application validates the official size and SHA-256 before publication.
On 4 GB GPUs the refinement adapter uses 512 px overlapping tiles and one
smaller-tile retry after CUDA OOM. It never falls back silently to CPU.
If refinement fails, the valid Real-ESRGAN result remains the delivery
fallback and is not labelled as refined.

## Content analysis

Content analysis is transversal and is not owned by the PDF feature.
`studio-media-api` publishes neutral operations for layout, mathematical
recognition, mathematical speech, visual description, contextual correction
and narratability classification. PDF supplies rendered regions and nearby text as one client;
theatre, imported images and future workspaces can submit the same requests.

- Qwen3-VL 4B runs through a private managed Ollama process bound only to
  loopback. Q8 is recommended with at least 12 GB of RAM; Q4 is the economical
  profile. A manual choice is persisted in the managed model store.
- PP-StructureV3 is imported as an explicit offline package. Its bridge refuses
  missing model directories and disables implicit Paddle model-source checks.
- MathCAT is an embedded offline MathML-to-speech engine. Its validated rules
  are materialized atomically from the application resources.

Qwen shares one runtime and model across description, correction and
narratability while publishing a separate neutral engine for each capability.
The narratability review accepts structured text without an image; one request
processes every region on a page and returns decisions by stable ID. It never
deletes text or replaces evidence.

Valid files produce at most `DEGRADED`. Qwen, PP-Structure and MathCAT become
`READY` only after a physical smoke remains valid for the current runtime,
model and hardware. Certifications live in
`state/document-ai-certifications`, outside projects.

The product-specific layers may persist a reviewable result, but they cannot
start runtimes, download models or import adapter-specific contracts directly.

## Compute queue

Voice and content analysis share `PriorityResourceScheduler`. Jobs request
CPU, GPU and model memory atomically. The default order is:

1. audio required to avoid a playback gap;
2. manually requested audio;
3. manual content analysis;
4. audio prefetch;
5. background document preparation.

Active inference is never preempted. XTTS yields between chunks; Qwen and
PP-Structure yield between requests. Qwen uses `keep_alive=0` and `/api/ps` to
confirm model release before another heavy engine starts. Settings shows the
active and queued admissions and allows cooperative cancellation.

The default capacity is one heavy model. Two concurrent heavy models are not
enabled until a hardware-specific concurrent smoke demonstrates at least 15 %
free RAM and VRAM.

## Academic tables

PP-Structure performs optional ROI-based table recognition. The deterministic
table layer stores cells and computes numeric facts; Qwen may only verbalize
facts grounded in cited cell IDs. Large tables require a summary or an
explicit row/column range. Every result remains `DRAFT` until review and never
changes the source PDF or canonical regions.

## Verification

```powershell
.\scripts\maintenance\audit-local-runtime.ps1
.\scripts\00-diagnosticar.bat
.\scripts\04-smoke-capacidades-reales.bat
```

Settings publishes adapter-owned install, import, repair, start/stop and smoke actions. The UI never accepts a destination, staging directory or process command: adapters select confined paths below the runtime root. License and redistribution checks remain mandatory before downloading or importing external binaries or model weights.
