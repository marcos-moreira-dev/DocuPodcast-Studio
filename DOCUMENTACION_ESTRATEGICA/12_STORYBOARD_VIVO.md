# Storyboard vivo

El storyboard vivo permite asociar imágenes del usuario a segmentos del guion y reproducir imagen + audio + texto activo.

## Alcance correcto

No es una película automática. No es un editor de video. No genera imágenes por IA.

La app permite:

- importar imágenes;
- asociarlas a segmentos;
- ver tarjetas de escenas;
- reproducir el audio mostrando la imagen correspondiente;
- exportar preview o paquete.

## MVP

- una imagen por segmento;
- canvas visual con tarjetas;
- selección de escena;
- sincronización con guion;
- resaltado durante playback;
- exportar preview PNG o paquete básico.

## Futuro

- varias imágenes por segmento;
- bindings por rango de texto;
- transiciones simples;
- video simple imagen fija + audio.

## Modelo

```text
StoryboardDocument
  ├── scenes
  ├── bindings
  └── layout

StoryboardBinding
  ├── id
  ├── segmentId
  ├── imageAssetId
  ├── displayMode
  ├── caption
  └── notes
```

## Regla importante

Mover una escena en el canvas no debe cambiar el orden narrativo del guion. El orden narrativo vive en `NarrationScriptDocument`; el canvas guarda layout visual.
