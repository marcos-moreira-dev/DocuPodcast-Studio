# Storyboard vivo

El storyboard vivo permite asociar imágenes aportadas por el usuario a segmentos del guion para reproducir imagen + audio + texto resaltado.

## Principio de alcance

No es una película. No es un editor de video profesional. No genera imágenes automáticamente.

El usuario decide qué imagen corresponde a qué segmento.

## MVP

- Importar imágenes.
- Asociar una imagen a un segmento.
- Mostrar tarjetas visuales por segmento.
- Resaltar escena activa durante reproducción.
- Reproducir audio del segmento correspondiente.
- Exportar preview o paquete básico.

## Modelo

```text
StoryboardDocument
  ├── scenes
  ├── bindings
  ├── layout
  └── validationIssues

StoryboardBinding
  ├── id
  ├── segmentId
  ├── imageAssetId
  ├── caption
  └── displayMode
```

## Canvas

El storyboard sí usa canvas visual inspirado en DMS:

- zoom;
- pan;
- fit-to-content;
- selección;
- drag de escenas;
- bounds de exportación;
- overlay de playback.

## Regla crítica

Mover una escena visual no cambia el orden narrativo del guion, salvo que exista una acción explícita de reordenar narrativa.
