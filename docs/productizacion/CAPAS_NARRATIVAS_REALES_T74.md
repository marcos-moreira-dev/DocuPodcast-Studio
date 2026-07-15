# Capas narrativas reales — T74

## Regla central

Las capas narrativas son artefactos del proyecto, no anotaciones escritas en Word, PDF, Markdown o TXT. Pero una capa productiva debe tener un target real cuando declara una relación con voz, imagen o audio.

## Matriz de targets

| Capa | Target válido | Comportamiento si falta |
|---|---|---|
| `VOICE` | `VoiceProfile.id` existente en `VoiceLibrary` | No asignar y pedir crear/importar voz. |
| `EMOTION` | `PerformanceStyle.id` existente en `VoiceLibrary` | No asignar y pedir restaurar/crear estilo. |
| `IMAGE` | `ProjectAssetReference` de tipo `IMAGE` o `THUMBNAIL` | No asignar y pedir importar/seleccionar imagen. |
| `HUMAN_AUDIO` | `ProjectAssetReference` de tipo `AUDIO_CLIP` o `AUDIO_FINAL` | No asignar y pedir importar/grabar audio. |
| `AMBIENT_AUDIO` | `ProjectAssetReference` de tipo `AUDIO_CLIP` o `AUDIO_FINAL` | No asignar y pedir importar/grabar audio ambiente. |
| `NOTE` | Sin target externo obligatorio | Crear nota interna del proyecto. |

## Impacto para usuario final

El usuario ya no debe creer que una imagen quedó asociada si en realidad solo se creó una promesa. Si pulsa asociar imagen sin haber importado/seleccionado una imagen, DocuPodcast debe decirlo claramente.

## Impacto técnico

El target se resuelve antes de instanciar `NarrativeLayerAssignment`. Esto evita persistir capas ambiguas y simplifica round-trip, integridad del proyecto, storyboard y video.
