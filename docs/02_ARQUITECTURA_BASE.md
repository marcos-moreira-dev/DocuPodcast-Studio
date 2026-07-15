# Arquitectura base

## Capas

```text
presentation → application → domain
                ↓
          infrastructure
```

## Reglas

- `domain` no importa JavaFX.
- `application` no importa JavaFX.
- `presentation` no instancia infraestructura concreta.
- `infrastructure` implementa puertos de aplicación.
- El motor TTS se usa mediante `AudioGenerationGateway`.

## Familias de servicios previstas

- Project
- Document
- ReadingProfile
- Narration/Script
- Voice
- Storyboard
- Audio
- Playback
- Export
- Observability

## Workspaces

- Welcome
- Document Reader
- Script Editor
- Storyboard
- Audio Jobs
- Voice Library
- Observability
