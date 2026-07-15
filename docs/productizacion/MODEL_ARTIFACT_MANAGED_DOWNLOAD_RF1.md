# MODEL-ARTIFACT-CONTRACT-RF1 + MANAGED-DOWNLOAD-RF1

## Estado

Implementado como base transversal sobre la tanda runtime/procesos.

## Corrección previa incluida

El diagnóstico `20260607-115938.zip` fallaba en `MotorGpuSmoke1SourceTest` porque `InspectXttsCudaSmokeUseCase` dejó de mencionar explícitamente `XttsCudaSmokeReport.MANIFEST_NAME` al pasar a `RuntimeArtifactPaths`. Se restauró el guardarraíl sin abandonar la política de rutas centralizada.

Además, se agregó una defensa Java adicional contra comandos legacy/localizados de Voz IA avanzada. Aunque el flujo administrado usa `scripts/tts/xtts-file-to-wav.ps1`, algunos settings antiguos pueden conservar comandos tipo `scripts/tts/Voz IA avanzada-file-to-wav.ps1` con `-ModelDir .../model.pth`. `LocalTtsProcessConfiguration` ahora normaliza el argumento `-ModelDir`/`--model-dir` antes de ejecutar el proceso. Esto evita que scripts antiguos reciban `model.pth` como carpeta y terminen en `model.pth/model.pth`.

## MODEL-ARTIFACT-CONTRACT-RF1

Se agregan contratos transversales en `application/runtime`:

- `ModelArtifactContract`
- `ModelArtifactRequirement`
- `RuntimeArtifactInspection`

Estos contratos describen archivos locales requeridos/opcionales para motores y herramientas sin duplicar listas en múltiples pantallas.

Contratos iniciales:

- `xttsAdvancedVoice()` para Voz IA avanzada.
- `piperLocalVoice()` para Voz local simple.
- `ffmpegVideoAudio()` para Video local/FFmpeg.

Regla: readiness, diagnóstico y packaging deben poder preguntar qué falta sin hardcodear cada vez `model.pth`, `piper.exe` o `ffmpeg.exe` en clases distintas.

## MANAGED-DOWNLOAD-RF1

Se agregan contratos base en `application/download`:

- `ManagedDownloadService`
- `DownloadRequest`
- `DownloadResult`
- `DownloadProgress`
- `DownloadResumePolicy`

Esta tanda no migra todas las descargas todavía. Deja el contrato transversal para que futuras migraciones de XTTS, Piper y FFmpeg compartan progreso, reanudación, cancelación y resultado.

## Criterios de aceptación cubiertos

- El smoke CUDA sigue usando nombre de manifiesto oficial.
- Comandos legacy de Voz IA avanzada ya no pasan `model.pth` como carpeta.
- Existen contratos comunes para XTTS, Piper y FFmpeg.
- Existe contrato transversal de descargas gestionadas.
- Tests focales pasan en entorno ChatGPT con stubs JUnit.

## Próximos pasos

- Integrar `ModelArtifactContract` directamente en readiness de XTTS/Piper/FFmpeg.
- Implementar infraestructura real de `ManagedDownloadService` para al menos FFmpeg o Piper.
- Continuar con `ENGINE-READINESS-UI-HF1` para mostrar estos estados con lenguaje humano.
