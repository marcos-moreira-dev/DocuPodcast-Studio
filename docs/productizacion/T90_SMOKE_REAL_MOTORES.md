# T90 — Smoke real de motores Coqui/Piper/FFmpeg

## Objetivo

T90 agrega un smoke **opt-in** para validar motores locales reales sin convertir la tanda en Release Candidate final. La suite normal sigue siendo verde aunque el usuario no tenga motores/modelos instalados; el smoke real se ejecuta explícitamente cuando el entorno ya tiene rutas y artefactos locales.

Alcance de producto:

- **Coqui/XTTS**: motor de calidad alta, debe generar un WAV corto con la voz por defecto.
- **Piper**: motor liviano de respaldo, debe generar un WAV corto.
- **FFmpeg**: soporte media, debe normalizar audio y extraer audio desde un video corto.

No se agregan capacidades nuevas a la UI ni se reintroduce audio a texto. Este smoke valida ejecución local y evidencia técnica de motores, no rediseño visual.

## Artefactos agregados

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/RealEnginesSmokeScenarioTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/RealEnginesSmokeT90SourceTest.java
scripts/19-smoke-motores-reales.bat
docs/testeo/SMOKE_REAL_MOTORES_T90.md
docs/132_TANDA_90_SMOKE_REAL_MOTORES.md
```

## Ejecución

Suite normal:

```bat
scripts\02-ejecutar-tests.bat
```

Smoke real:

```bat
scripts\19-smoke-motores-reales.bat
```

El test `RealEnginesSmokeScenarioTest` se omite por defecto. Solo se activa con:

```text
-Ddocupodcast.realEnginesSmoke.enabled=true
```

El script público ya pasa esa propiedad.

## Evidencia

La evidencia se genera en:

```text
target/docupodcast-real-engines-smoke/
```

Contenido esperado:

```text
T90_REAL_ENGINES_SMOKE_REPORT.md
input/
output/
logs/
```

## Rutas por defecto

```text
scripts/tts/xtts-file-to-wav.ps1
tools/xtts-wrapper/synthesize_xtts.py
models/tts/xtts
models/tts/xtts/speakers/voz-por-defecto.wav
scripts/tts/piper-file-to-wav.ps1
tools/piper/piper.exe
models/tts/piper/voices/*.onnx
tools/ffmpeg/bin/ffmpeg.exe
```

## Personalización de rutas

Se pueden usar propiedades Maven o variables de entorno para modelos/herramientas, excepto el Python de Coqui/XTTS, que siempre debe ser repo-local:

```text
docupodcast.smoke.modelsRoot
DOCUPODCAST_MODELS_ROOT
docupodcast.smoke.piper.exe
DOCUPODCAST_PIPER_EXE
docupodcast.smoke.piper.model
DOCUPODCAST_PIPER_MODEL
Coqui/XTTS usa exclusivamente el Python local administrado por DocuPodcast:
`tools/xtts-wrapper/.venv/Scripts/python.exe`.
No se permite sobrescribirlo con Python global ni PATH.
docupodcast.smoke.xtts.modelDir
DOCUPODCAST_XTTS_MODEL_DIR
docupodcast.smoke.xtts.speaker
DOCUPODCAST_XTTS_SPEAKER_WAV
docupodcast.smoke.ffmpeg
DOCUPODCAST_FFMPEG
```

Motores obligatorios por defecto:

```text
coqui,piper,ffmpeg
```

Se puede ajustar con:

```text
-Ddocupodcast.realEnginesSmoke.required=coqui,ffmpeg
```

## Criterio de aceptación

- Coqui/XTTS genera un WAV real.
- Piper genera un WAV real.
- FFmpeg genera fixture de audio/video, normaliza audio y extrae audio de video.
- Cada proceso deja stdout/stderr bajo `logs/`.
- El reporte explica qué pasó sin llevar detalles técnicos al Documento.

## Alcance explícito

T90 no es Release Candidate final. Es la primera evidencia local real de motores. El RC final queda para una tanda posterior, cuando también estén cerrados comandos, GUI final, packaging, licencias/manifiestos y smoke visual.
