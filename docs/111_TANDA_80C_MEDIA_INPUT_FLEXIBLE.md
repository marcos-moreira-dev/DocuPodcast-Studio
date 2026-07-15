# Tanda 80C — Entrada flexible de media: MP3/WAV/video→audio

T80C amplía el cerebro de DocuPodcast para que la acción operativa sea simple: **Asignar audio**. El usuario puede elegir MP3 o WAV directamente, y también puede elegir un video común solo para extraer su audio. El video no se convierte en editor de video; queda registrado como procedencia del audio derivado.

## Contrato de producto

- La interfaz principal no debe dividir acciones en “seleccionar efecto de sonido”, “seleccionar humano hablando” u otras variantes redundantes.
- El usuario es responsable de elegir y nombrar archivos de forma coherente con su uso: `Lucía hablando.mp3`, `pajaritos cantando.wav`, `ambiente bosque.mp3`.
- La pantalla Documento conserva lenguaje humano y una sola acción: `Asignar audio`.
- Los detalles técnicos —FFmpeg, extracción, rutas, cómputo, logs— viven en Configuración/Diagnóstico, la **bodega técnica**.

## Formatos aceptados

- Audio directo: MP3 y WAV.
- Video para extraer audio: MP4, MOV, MKV y WEBM.

## Cambios de cerebro

- Nuevo paquete `application.media` con clasificación de formatos, importación de media de usuario y gateway de extracción.
- Nuevo paquete `infrastructure.media` con repositorio local de media y adaptador FFmpeg.
- Nuevo `ProjectAssetKind.VIDEO_SOURCE` para conservar el video original como procedencia.
- El audio extraído se registra como `AUDIO_CLIP`, por lo que puede ser asignado a texto igual que un audio directo.

## Reglas de persistencia

La T80B queda como prerequisito: todo se guarda dentro de la carpeta contenedora del proyecto. T80C usa:

- `media/audio/` para MP3/WAV importados y WAV derivados desde video.
- `media/video/` para videos originales usados como fuente.

## Limitaciones honestas

- La extracción desde video requiere FFmpeg configurado o embebido.
- Si FFmpeg no está disponible, el cerebro bloquea la importación de video y explica que falta FFmpeg.
- La UI final de T81 debe mostrar una nota no invasiva, no tecnicismos.
