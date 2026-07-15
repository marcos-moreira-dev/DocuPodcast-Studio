# 07 — Arquitectura objetivo

La arquitectura debe ser por capas:

```text
bootstrap
presentation
application
domain
infrastructure
```

## Bootstrap

Arranca la aplicación, compone servicios, devuelve `ApplicationRuntime` y no conoce detalles de negocio.

## Presentation

JavaFX, ViewModels, Shell, Workspaces, Toolbar, SideDock, dialogs, CSS y bindings. No debe importar infrastructure directamente.

## Application

Casos de uso, fachadas por familia y puertos. No debe conocer JavaFX.

Familias esperadas:

```text
ProjectApplicationServices
DocumentApplicationServices
ReadingProfileApplicationServices
NarrationApplicationServices
VoiceApplicationServices
StoryboardApplicationServices
AudioApplicationServices
PlaybackApplicationServices
ExportApplicationServices
ObservabilityApplicationServices
GuideApplicationServices
```

## Domain

Modelo puro: proyecto, documento, guion, voz, storyboard, audio, playback, assets.

## Infrastructure

Adaptadores concretos: DOCX importer, JSON repository, TTS worker, audio merger, exporters, media importers.

## Regla de dependencia

```text
presentation → application → domain
infrastructure → application/domain ports
bootstrap → compone todo
```

La UI nunca debe llamar directamente al motor TTS.
