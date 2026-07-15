# Guion narrable

El guion narrable es el centro del producto. No es el Word original ni un canvas. Es un documento estructurado editable que conserva la relación entre texto, voz, estilo, personaje, imagen, audio y job.

## Modelo base

```text
NarrationScriptDocument
  ├── sections
  ├── segments
  ├── characters
  ├── voiceProfiles
  ├── performanceStyles
  ├── performanceSpans
  ├── storyboardBindings
  ├── audioReferences
  └── validationIssues
```

## Segmento narrable

Un segmento es una unidad humana de narración. Puede venir de un párrafo, una sección, una línea de diálogo o una agrupación lógica.

Campos esperados:

- ID estable: `SEG-001`.
- Título o resumen.
- Texto.
- Bloques fuente del documento.
- Voz asignada.
- Personaje asignado.
- Estilo de interpretación.
- Pausas.
- Imagen asociada.
- Audio generado.
- Estado.

## Selección

El usuario debe poder seleccionar:

- segmento completo;
- rango de texto;
- personaje;
- voz;
- estilo;
- imagen;
- clip de audio.

Modelo recomendado: `ScriptSelection` con `kind`, `id`, `ownerId` y rango opcional.

## Reglas

- El guion no se edita como canvas.
- El texto largo se edita en el centro.
- Los metadatos se editan en SideDock/inspector.
- Los cambios deben marcar el proyecto dirty.
- Cargar el proyecto no debe marcar dirty.
