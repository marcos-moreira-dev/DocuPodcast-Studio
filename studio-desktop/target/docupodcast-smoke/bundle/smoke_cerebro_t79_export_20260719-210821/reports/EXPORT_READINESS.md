# Preparación de exportaciones DocuPodcast

- Proyecto: Smoke cerebro T79
- Estado general: Exportable
- Fecha UTC: 2026-07-20T02:08:21.517002900Z
- Salidas exportables: 7
- Salidas bloqueadas: 0

Este reporte pertenece al cerebro de exportación: declara qué puede salir, qué falta y qué no se promete. No depende de la interfaz gráfica y evita botones o promesas sin cadena real.

| Salida | Formato | Estado | Destino sugerido |
|---|---|---|---|
| Paquete completo auditable | Carpeta | Exportable | `carpeta-exportacion/` |
| Reporte diagnóstico | Markdown | Exportable | `reporte-diagnostico.md` |
| Podcast WAV / Audio final WAV | WAV | Exportable | `audio-final.wav / .mp3 / .aac` |
| Video documental texto+audio | MP4 | Exportable | `estudio-documental-texto-audio.mp4` |
| Video MP4 final | MP4 | Exportable | `video-final.mp4` |
| Panel visual / imágenes | Markdown | Exportable | `resumen_visual.md` |
| Paquete de video simple auditable | Paquete de video simple | Exportable | `video-simple/` |

## Paquete completo auditable

- Estado: Exportable
- Formato: Carpeta
- Destino sugerido: `carpeta-exportacion/`

### Evidencia

- El proyecto tiene archivo .docupodcast guardado.

### Faltantes

- Ninguno.

### Limitaciones honestas

- El paquete no instala motores ni modelos; solo empaqueta entrada, editable, salidas, assets, jobs y reportes.

## Reporte diagnóstico

- Estado: Exportable
- Formato: Markdown
- Destino sugerido: `reporte-diagnostico.md`

### Evidencia

- Proyecto cargado: Smoke cerebro T79.

### Faltantes

- Ninguno.

### Limitaciones honestas

- El reporte diagnóstico describe estado y advertencias; no repara archivos por sí solo.

## Podcast WAV / Audio final WAV

- Estado: Exportable
- Formato: WAV
- Destino sugerido: `audio-final.wav / .mp3 / .aac`

### Evidencia

- Hay audio exportable para WAV final; MP3/AAC se comprimen desde WAV usando Video local/FFmpeg.

### Faltantes

- Ninguno.

### Limitaciones honestas

- Solo se exporta audio realmente existente; no se sintetizan fragmentos faltantes durante la exportación.

## Video documental texto+audio

- Estado: Exportable
- Formato: MP4
- Destino sugerido: `estudio-documental-texto-audio.mp4`

### Evidencia

- Lectura preparada y audio listo para 3 fragmento(s) narrable(s).

### Faltantes

- Ninguno.

### Limitaciones honestas

- Los frames de texto se generan temporalmente en exports/document-study-frames y no se guardan como assets.

## Video MP4 final

- Estado: Exportable
- Formato: MP4
- Destino sugerido: `video-final.mp4`

### Evidencia

- Hay 1 frame(s) con visual y audio listo para MP4 final.

### Faltantes

- Ninguno.

### Limitaciones honestas

- Video local/FFmpeg se valida al iniciar el render; si no está disponible, se mostrará un bloqueo operativo.

## Panel visual / imágenes

- Estado: Exportable
- Formato: Markdown
- Destino sugerido: `resumen_visual.md`

### Evidencia

- Panel visual cargado con 1 imágenes asociadas.

### Faltantes

- Ninguno.

### Limitaciones honestas

- El resumen documenta asociaciones; el render de video se prepara por el paquete de video simple.

## Paquete de video simple auditable

- Estado: Exportable
- Formato: Paquete de video simple
- Destino sugerido: `video-simple/`

### Evidencia

- Lectura preparada, panel visual y audio exportable están disponibles.

### Faltantes

- Ninguno.

### Limitaciones honestas

- La exportación prepara contrato de render; la ejecución real de FFmpeg permanece auditable y cancelable.
