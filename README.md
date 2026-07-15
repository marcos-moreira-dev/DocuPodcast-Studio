# DocuPodcast Studio

DocuPodcast Studio is a local-first JavaFX application for turning documents and scripts into narrated audio, visual projects and final video. It supports three product categories:

- Documentary Studio for DOCX/PDF study material.
- Narrative Video for narrated audiovisual projects.
- Theatre for scenes, characters, voices, camera references and theatrical video.

The application stores project media beside each `.docupodcast.json` file. AI, TTS, image and video tools run locally; normal operation does not require an internet API.

## Requirements

- Windows x86_64.
- Temurin JDK 21.
- Maven 3.9 or the configured Maven wrapper/toolchain.
- Local engines and models described in `runtime/local-runtime-manifest.json`.

## Build and run

```powershell
mvn clean test
mvn javafx:run
```

Useful checks:

```powershell
.\scripts\maintenance\audit-local-runtime.ps1
.\scripts\maintenance\clean-workspace.ps1
```

The cleanup command is a dry-run unless `-Apply` is supplied. Do not use `git clean -xfd`: engines and model weights are intentionally ignored by Git.

## Repository map

- `src/main/java`: application source.
- `src/test/java`: unit, integration and architecture tests.
- `src/main/resources/examples`: official examples shipped with the application.
- `models`, `tools`: local ignored runtimes and weights.
- `scripts`: setup, diagnostics, packaging and maintenance.
- `runtime`: runtime contracts and local asset manifest.
- `docs`: current documentation only.

Start with [the documentation index](docs/README.md) and [the architecture overview](docs/architecture/overview.md).

