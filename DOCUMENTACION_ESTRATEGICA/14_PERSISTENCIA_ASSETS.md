# Persistencia y assets

La persistencia editable es `.docupodcast.json` + carpeta de proyecto.

## Estructura

```text
MiProyecto/
├── MiProyecto.docupodcast.json
├── source/
├── document/
├── script/
├── voices/
├── media/
├── storyboard/
├── jobs/
└── exports/
```

## Regla

El JSON referencia. La carpeta contiene. El catálogo valida.

No guardar audios/imágenes como Base64 dentro del JSON.

## Assets

```text
ProjectAssetReference
  ├── id
  ├── kind
  ├── displayName
  ├── relativePath
  ├── mimeType
  ├── purpose
  ├── checksum
  └── notes
```

Kinds:

- SOURCE_DOCUMENT;
- IMAGE;
- THUMBNAIL;
- VOICE_SAMPLE;
- AUDIO_CLIP;
- AUDIO_FINAL;
- AUDIO_MANIFEST;
- STORYBOARD_MANIFEST;
- GENERATION_LOG;
- EXPORT;
- OTHER.

## Seguridad de rutas

Rechazar:

- rutas absolutas;
- `..`;
- URLs;
- `file:`;
- rutas fuera de la carpeta del proyecto.
