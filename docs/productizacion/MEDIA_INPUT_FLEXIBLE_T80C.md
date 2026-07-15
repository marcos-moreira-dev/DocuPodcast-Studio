# Media input flexible T80C

## Decisión

DocuPodcast debe mantener una experiencia de supermercado para el usuario y una configuración avanzada separada para configuración. En el lector principal no se muestran categorías técnicas ni botones redundantes. La acción es **Asignar audio**.

## Formatos

- MP3: audio importado como `AUDIO_CLIP`.
- WAV: audio importado como `AUDIO_CLIP`.
- MP4/MOV/MKV/WEBM: video aceptado solo para extraer audio mediante FFmpeg.

## UX esperada

Microcopy sugerida para T81:

> Consejo: elige archivos con nombres claros para recordar qué representan: voz, ambiente, música o efecto.

No se debe usar un botón de seleccionar efecto de sonido separado de un botón de seleccionar humano hablando. El archivo y su nombre dan contexto; el cerebro solo necesita registrar un asset de audio asignable.

## Cerebro

- `UserMediaFormatPolicy`: clasifica audio directo, video para extracción o formato no soportado.
- `ImportUserMediaAssetUseCase`: importa MP3/WAV o extrae audio desde video.
- `UserMediaAssetRepository`: guarda assets dentro de la carpeta contenedora.
- `VideoAudioExtractionGateway`: frontera para FFmpeg u otro extractor.
- `ProjectAssetKind.VIDEO_SOURCE`: conserva procedencia del video original.

## Guardarraíl

La función principal no debe exponer FFmpeg, codec, GPU ni rutas en Documento. Eso pertenece a Configuración/Diagnóstico.
