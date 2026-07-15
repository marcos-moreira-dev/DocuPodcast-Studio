# 16 — Persistencia `.docupodcast.json`, assets y jobs

La persistencia debe tomar como referencia `.dms`, pero adaptarse a audio e imágenes.

## Frontera

```text
DOCX/PDF/MD = entrada
Markdown DocuPodcast = intercambio humano/IA
.docupodcast.json = proyecto editable
assets/ = recursos pesados
jobs/ = generación reanudable
exports/ = salidas
```

## Estructura

```text
MiProyecto/
  MiProyecto.docupodcast.json
  source/
  document/
  script/
  voices/
  media/
  storyboard/
  jobs/
  exports/
```

## ProjectAssetReference

Todos los assets deben usar rutas relativas seguras. Rechazar:

```text
rutas absolutas
URLs
.. que escapen del proyecto
file:
http:
https:
```

## Asset kinds

```text
SOURCE_DOCUMENT
IMPORTED_DOCUMENT
IMAGE
THUMBNAIL
VOICE_SAMPLE
AUDIO_CLIP
AUDIO_FINAL
AUDIO_MANIFEST
STORYBOARD_MANIFEST
GENERATION_LOG
EXPORT
OTHER
```

## JSON principal

Debe referenciar subdocumentos:

```text
source
document
script
voices
storyboard
audio
assets
view
jobs
exports
```

No meter WAV/MP3 como Base64.
