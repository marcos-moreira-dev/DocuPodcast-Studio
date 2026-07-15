# Arquitectura final propuesta

DocuPodcast Studio combina la carcasa de DMS con el motor de jobs largos de Fractal y un dominio propio.

## Capas

```text
presentation
application
domain
infrastructure
bootstrap
```

## Dependencias permitidas

```text
presentation → application → domain
infrastructure → application/domain ports
bootstrap → todos para composición
```

## Dependencias prohibidas

```text
domain → JavaFX
domain → infrastructure
application → JavaFX
presentation → infrastructure directa
UI → motor TTS directo
```

## Servicios por familia

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

## Infraestructura principal

```text
DocuPodcastProjectFileRepository
DocxDocumentImporter
MarkdownScriptImporter
LocalMediaAssetRepository
VoiceProfileFileRepository
AudioJobFileRepository
GenerationLogJsonlRepository
AudioGenerationGateway
AudioMerger
PodcastExporter
StoryboardExporter
```

## Principio central

La UI presenta una aplicación única. La tecnología de TTS puede ser Java puro, proceso local, Python empaquetado, Piper, XTTS u otra opción, pero debe quedar detrás de `AudioGenerationGateway`.

## Modelo mental

```text
Word/DOCX
  → ReadableDocument
  → NarrationScriptDocument
  → Voice/Style/Storyboard annotations
  → AudioJob
  → AudioManifest + Podcast final
  → PlaybackManifest + Storyboard vivo
```
