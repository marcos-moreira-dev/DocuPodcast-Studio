# T109 — Overlay de procesos largos implementado

## Base

T109 parte de T107 verde validada localmente y aplica feedback visual del Documento antes de continuar con superficies secundarias.

## Cambios

### Rail derecho

- Se elimina la seccion redundante `Asignadas`.
- El rail queda como lista visual de `Storyboard` e `Imagenes`.
- La accion destructiva `Borrar imagenes` se conserva como accion del panel Visual.
- Al hacer clic en una tarjeta asociada, el texto se selecciona y el sidebar izquierdo se actualiza con el fragmento y su imagen.

### Sidebar izquierdo sincronizado desde rail

`selectDocumentBlockForStoryboardSegment(...)` ya no solo resalta bloque. Ahora intenta seleccionar el rango documental de la capa de imagen o, si no existe, el bloque completo del segmento. Esto actualiza:

```text
selectedDocumentBlockId
selectedDocumentTextRange
selectedDocumentRangeLabel
selectedDocumentTextPreview
documentMediaRevisionProperty
```

Con esto, Texto/Audio/Imagen del sidebar izquierdo reaccionan a la tarjeta elegida en el rail derecho.

### Overlay de procesos largos

Se agrega `presentation.process.LongProcessOverlayView`.

- Se monta sobre `workspaceHost` en `DocuPodcastShellView`.
- Muestra progreso de audio, ETA y contador de segmentos.
- Permite cancelar el job activo.
- No navega a `AUDIO_JOBS`.
- Oculta detalles tecnicos y logs del flujo principal.

### ViewModel

Los flujos de audio dejan de empujar el workspace a `AUDIO_JOBS`. Si falta proyecto o guion, se comunica por status y se mantiene Documento como superficie principal.

## No incluido

- No implementa todavia overlay persistente para FFmpeg/setup/video render.
- No elimina clases historicas de AudioWorkspace, que quedan como soporte tecnico no montado.
- No cierra el refactor pendiente `DocumentMediaWorkflowCoordinator`.

## Siguiente

T108 — Workspace Voces final, salvo que nuevas capturas requieran otro ajuste visual intermedio.
