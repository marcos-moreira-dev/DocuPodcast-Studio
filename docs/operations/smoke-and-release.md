# Smoke tests and release

El lector PDF semántico congelado tiene una guía separada de [smokes físicos y corpus final](pdf-semantic-reader-smokes.md). Esas pruebas son opt-in y nunca convierten archivos o runtimes externos en requisitos de `mvn test`.

## Local validation

1. Run `scripts\03-verificar-completo.bat`.
2. Para certificar motores instalados (optativo y fuera del onboarding minimo), ejecuta `scripts\04-smoke-capacidades-reales.bat`; los motores seleccionados deben pasar. Puede tardar horas.
3. Open one official example in each category.
4. Verify document selection, audio playback and project save/reopen.
5. Export Documentary, Narrative and Theatre video.
6. Inspect SideDocks, dialogs, scroll and canvas layout at common desktop sizes.

The real smoke has two explicit tiers:

- `scripts\04-smoke-capacidades-reales.bat --tier=short` detects installed Piper, XTTS, FFmpeg, SD 1.5, RIFE and OCR resources and produces bounded real evidence.
- `scripts\04-smoke-capacidades-reales.bat --tier=soak --engine=comfyui --preset=flux-kontext --timeout-hours=6` certifies exactly one long preset. Soak refuses to start without both selectors.

Selectors `--engine=`, `--preset=` and `--required=` isolate work. `--resume` reuses the latest session and skips a prior `PASS` only when its signature still matches. Evidence lives under `target/certification/<timestamp>` and `target/certification/latest.txt` points to the newest session.

Image certification also accepts `--prompt=...` or `--prompt-file=...`, plus `--label`, `--seed`, `--width` and `--height`. The exact prompt participates in the resume signature and is copied into the evidence directory; changing it always creates a distinct certification result.

WAN/LTX workflows that are absent are recorded as `RESOURCE_MISSING`; they are never reported as a successful real generation. `--engine=rife` runs a real middle-frame interpolation but RIFE remains an auxiliary preparation capability, not a selectable engine.

The certification only stops a ComfyUI process that it started through engine administration. It never terminates unrelated user processes. Long jobs are bounded to six hours.

`01-ejecutar-app.bat --smoke` is the automated production-launch path: it must show the real shell and exit cleanly. An older application instance must be closed before validating a new build.

## Packaging scope

`05-generar-app-image.bat` creates a thin Java app-image from `studio-launcher`. It includes Java modules, Logback, stylus native support and the three small TTS helpers. It excludes models, Python environments, ComfyUI and other heavy runtimes.

No MSI is produced in this tranche. A future MSI must wrap the thin app only; Settings downloads or imports static resources once into the writable runtime. Third-party manifests and redistribution terms remain mandatory before distributing external components.
