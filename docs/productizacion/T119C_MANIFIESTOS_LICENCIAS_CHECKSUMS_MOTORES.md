# T119C — Manifiestos, licencias y checksums de motores

## Propósito

Esta tanda formaliza el contrato de artefactos de motores antes de cerrar una Release Candidate final. El objetivo no es meter binarios o modelos sin control, sino dejar una frontera auditable: cada ejecutable, modelo, voz, runtime o script relevante debe tener ruta esperada, licencia, origen y SHA-256 antes de ser distribuido como producto final.

## Decisiones de producto

- FFmpeg y FFprobe son obligatorios para RC final.
- Coqui/XTTS es obligatorio como motor principal de lectura de calidad alta.
- Python debe ser local/portable para Coqui/XTTS; no se usa Python global ni PATH del sistema.
- La voz neutral prediseñada es obligatoria y protegida.
- Piper queda como modo intermedio/liviano; si se distribuye, también requiere licencia y checksum.
- Whisper/STT no forma parte del producto.

## Nuevos contratos

Se agregan contratos en `application.runtime`:

- `EngineArtifactKind`
- `EngineArtifactDescriptor`
- `EngineArtifactManifest`
- `EngineArtifactValidationReport`
- `BuildEngineArtifactManifestUseCase`
- `ValidateEngineArtifactManifestUseCase`

El manifiesto enumera artefactos como:

- `tools/ffmpeg/bin/ffmpeg.exe`
- `tools/ffmpeg/bin/ffprobe.exe`
- `tools/xtts-wrapper/.venv/Scripts/python.exe`
- `tools/xtts-wrapper/synthesize_xtts.py`
- `models/tts/xtts/config.json`
- `models/tts/xtts/*.pth` o `*.safetensors`
- `models/tts/xtts/vocab.json` o `vocab.txt`
- `models/tts/xtts/speakers/voz-por-defecto.wav`
- `tools/piper/piper.exe`
- `models/tts/piper/voices/*.onnx`
- `models/tts/piper/voices/*.onnx.json`

## Checksums

El descriptor usa el valor sentinel:

```text
PENDING_SHA256_UNTIL_ARTIFACT_LOCKED
```

Esto significa que el contrato existe, pero la RC final todavía no puede cerrarse hasta reemplazarlo por un SHA-256 concreto de cada artefacto obligatorio.

## Licencias

Cada artefacto debe registrar licencia o estado legal. Si la licencia dice `Pendiente`, `ValidateEngineArtifactManifestUseCase` lo considera bloqueo para RC final cuando el artefacto es obligatorio.

## Script actualizado

`scripts/30-generar-manifest-terceros.bat` ahora genera:

- `target/legal/THIRD_PARTY_MANIFEST.md`
- `target/legal/ENGINE_ARTIFACTS_MANIFEST.md`
- copias en `dist/legal/`

El segundo manifiesto no sustituye el contrato Java, pero deja un reporte legible para humanos y para empaquetado.

## Criterio de aceptación

- Existe contrato de artefactos de motores.
- FFmpeg, FFprobe, Python local, XTTS, voz neutral y Piper quedan inventariados.
- Los artefactos obligatorios fallan validación RC final si no tienen SHA-256 concreto.
- Los scripts generan manifiesto de artefactos.
- No se reintroduce Whisper/STT.
