# Persistencia y assets

DocuPodcast usa `.docupodcast.json` como proyecto editable y una carpeta de recursos para archivos pesados.

## Estructura propuesta

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

## Assets

Tipos previstos:

- SOURCE_DOCUMENT;
- IMPORTED_DOCUMENT;
- IMAGE;
- THUMBNAIL;
- VOICE_SAMPLE;
- VOICE_MODEL;
- AUDIO_CLIP;
- AUDIO_FINAL;
- AUDIO_MANIFEST;
- STORYBOARD_MANIFEST;
- GENERATION_LOG;
- EXPORT;
- OTHER.

## Rutas

El proyecto debe guardar rutas relativas.

Rechazar:

- rutas absolutas;
- URLs;
- `..`;
- rutas que escapen del proyecto.

## Versionado

El JSON debe tener `formatVersion` y rechazar versiones futuras.

## Validación

El payload debe validar:

- script requerido si el proyecto es de guion;
- storyboard con segmentos existentes;
- audio clips con segmentos existentes;
- voces referenciadas existentes;
- imágenes referenciadas existentes.
