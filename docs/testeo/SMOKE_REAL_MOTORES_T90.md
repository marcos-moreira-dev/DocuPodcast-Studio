# Smoke real de motores — T90

## Propósito

Validar que el entorno local puede ejecutar los motores reales que pertenecen al producto DocuPodcast:

- Coqui/XTTS para voz de calidad alta.
- Piper como respaldo liviano.
- FFmpeg para preparar audio/video.

## Preparación

Colocar o configurar:

```text
tools/xtts-wrapper/synthesize_xtts.py
models/tts/xtts/
models/tts/xtts/speakers/voz-por-defecto.wav
tools/piper/piper.exe
models/tts/piper/voices/*.onnx
tools/ffmpeg/bin/ffmpeg.exe
```

## Comando

```bat
scripts\19-smoke-motores-reales.bat
```

## Evidencia esperada

```text
target/docupodcast-real-engines-smoke/T90_REAL_ENGINES_SMOKE_REPORT.md
target/docupodcast-real-engines-smoke/output/coqui-xtts-smoke.wav
target/docupodcast-real-engines-smoke/output/piper-smoke.wav
target/docupodcast-real-engines-smoke/output/ffmpeg-normalized.wav
target/docupodcast-real-engines-smoke/output/ffmpeg-extracted-from-video.wav
target/docupodcast-real-engines-smoke/logs/
```

## Interpretación

- Si el smoke no está habilitado, el test queda omitido y deja reporte de omisión.
- Si un motor obligatorio falta, el smoke falla y el reporte dice qué ruta falta.
- Si un motor genera WAV, el reporte marca OK y conserva evidencia.

## Límite

Este smoke no valida la interfaz gráfica ni empaquetado final. Tampoco reemplaza la suite normal de tests.
