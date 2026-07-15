# T106 - Sidebar izquierdo contextual final

## Objetivo

Cerrar el sidebar izquierdo como inspector/configurador del fragmento seleccionado antes de avanzar al rail derecho final.

## Cambios

- El modulo principal pasa a llamarse Fragmento.
- El rail izquierdo usa Texto, Audio e Imagen como accesos legibles.
- Fragmento ofrece acciones rapidas: reproducir desde aqui, voz, emocion e imagen.
- Audio reduce jerga: voz disponible, preparar audio, audio local.
- Imagen explica que las miniaturas quedan en el panel Visual.
- La playbar conserva transparencia pero pasa a glass gris oscuro para contrastar con botones blancos.
- Los tests de rail/documentacion visual se alinean con las etiquetas vigentes desde T109: Storyboard e Imagenes.

## Fuera de alcance

- No se implementa aun rail derecho redimensionable.
- No se implementa playback por unidad/oracion end-to-end.
- No se cambia el cerebro de capas, audio ni storyboard.
- No se implementa overlay de procesos largos.

## Validacion recomendada

```bat
scripts\99-diagnostico-completo.bat
```

Si queda verde, continuar con T107.
