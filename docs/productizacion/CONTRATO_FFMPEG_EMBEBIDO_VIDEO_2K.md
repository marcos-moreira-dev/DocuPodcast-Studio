# Contrato FFmpeg embebido y video 2K

Este contrato corrige la ambigüedad anterior: la exportación de video no es una función opcional en el sentido de prescindible. Es opcional solo para el usuario que no usa storyboard, imágenes o frames. Si el usuario activa storyboard o asocia imágenes a texto, DocuPodcast Studio debe poder exportar video.

## Ubicación esperada

La distribución final debe incluir FFmpeg de forma local/autocontenida cuando la licencia y el paquete de distribución lo permitan:

```text
DocuPodcast Studio/
  app/
  runtime/
  tools/
    ffmpeg/
      bin/
        ffmpeg.exe
        ffprobe.exe
      LICENSE.txt
      VERSION.txt
      MANIFEST.txt
```

La aplicación debe buscar primero el FFmpeg embebido:

```text
tools/ffmpeg/bin/ffmpeg.exe
tools/ffmpeg/bin/ffprobe.exe
```

Solo si no existe, se puede permitir configuración externa desde Configuración avanzada. No se debe exigir al usuario modificar `PATH` ni instalar FFmpeg globalmente.

## Resolución de video

La exportación por defecto debe apuntar a mínimo 2K.

```text
Resolución base recomendada: 2560x1440
Relación por defecto: 16:9
Fallback: degradación explícita si imágenes fuente no alcanzan calidad suficiente
```

Si una imagen fuente es pequeña, la app puede escalarla o usar fondo/letterbox, pero debe advertir que la calidad final depende de la imagen original.

## Flujo esperado

```text
Documento con audio + storyboard/imágenes
→ plan de frames
→ duración por frame = audio + silencio configurado
→ render con ffmpeg embebido
→ MP4 2K
→ reporte y manifest
```

## Fallback

Si FFmpeg embebido no está presente o falla:

```text
exportar paquete preparado;
incluir frames.csv;
incluir ffmpeg-concat.txt;
incluir script render-video-simple.bat;
incluir reporte con causa;
no fallar de forma opaca.
```

## Tests obligatorios futuros

```text
EmbeddedFfmpegDiscoveryTest
VideoExportUsesBundledFfmpegTest
FfmpegPackagingManifestSourceTest
FfmpegFallbackConfigurationTest
SimpleVideo2KExportPolicyTest
SimpleVideoFallbackPackageTest
```

## Tanda 58 — Video simple 2K + FFmpeg embebido

- Video simple es capacidad del producto cuando el usuario usa storyboard/imágenes.
- Resolución predeterminada: 2K (2560x1440); opciones: 720p, 1080p, 2K y 4K.
- FFmpeg se busca primero como herramienta embebida en `tools/ffmpeg/bin/ffmpeg.exe` y no debe exigir PATH global.
- Durante render, la pantalla operativa entra en modo render con progreso y bloqueo temporal de lectura/edición/nuevas exportaciones.
