# ADR 0001: Local-first runtimes remain beside the application

## Status

Accepted.

## Decision

DocuPodcast Studio keeps FFmpeg, Piper, XTTS, ComfyUI and model weights inside the application workspace or portable installation. They are ignored by Git but described by a tracked manifest and verified by maintenance scripts.

## Consequences

- Generation, TTS and media rendering do not require an internet API during normal use.
- A source checkout is smaller than a complete operational installation.
- Setup and release procedures must prepare ignored artifacts explicitly.
- Generic `git clean` commands are unsafe and prohibited for this workspace.
- Official examples remain tracked because they are part of the product experience, not runtime cache.
