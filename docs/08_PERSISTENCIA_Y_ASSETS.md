# Persistencia y assets

## Formato

Proyecto editable:

```text
MiProyecto.docupodcast.json
```

Carpeta asociada:

```text
source/
document/
script/
voices/
media/
storyboard/
jobs/
exports/
```

## Reglas

- Usar rutas relativas dentro del proyecto.
- No guardar WAV/MP3 como Base64 en el JSON.
- No incluir modelos TTS gigantes por defecto.
- Rechazar rutas absolutas en assets del proyecto.
- Guardar jobs reanudables.

## Assets previstos

- SOURCE_DOCUMENT
- IMAGE
- THUMBNAIL
- VOICE_SAMPLE
- AUDIO_CLIP
- AUDIO_FINAL
- AUDIO_MANIFEST
- STORYBOARD_MANIFEST
- GENERATION_LOG
- EXPORT
