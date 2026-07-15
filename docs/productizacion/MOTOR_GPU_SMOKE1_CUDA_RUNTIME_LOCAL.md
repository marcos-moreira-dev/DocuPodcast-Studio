# MOTOR-GPU-SMOKE1 — GPU real dentro del Python autocontenido

## Estado

Implementada sobre `MOTOR-ADV-READY-GATE1` verde.

## Problema corregido

La app podía detectar una GPU NVIDIA en Windows y dejar al usuario elegir una política con GPU, pero Voz IA avanzada seguía sin una prueba real de CUDA dentro del Python autocontenido de DocuPodcast. Tener GPU instalada no implica que el entorno `tools/xtts-wrapper/.venv` tenga PyTorch con CUDA usable.

## Decisión de producto

- GPU detectada por Windows = candidata.
- GPU confirmada para Voz IA avanzada = solo después de smoke CUDA dentro del Python local.
- Si el smoke no confirma CUDA, Voz IA avanzada sigue usando CPU de forma explícita.
- La app no usa Python global para esta prueba.

## Cambios principales

### Application / compute

Se agregan:

- `XttsCudaSmokeReport`
- `InspectXttsCudaSmokeUseCase`
- `RunXttsCudaSmokeUseCase`
- `XttsCudaRuntimeProbeGateway`
- `XttsCudaRuntimeProbeResult`
- `ProcessXttsCudaRuntimeProbeGateway`

El smoke ejecuta el Python local y consulta:

```bat
tools\xtts-wrapper\.venv\Scripts\python.exe -c "import torch; print(torch.cuda.is_available()); print(torch.version.cuda)"
```

En la implementación real se guarda un manifiesto en:

```text
runtime/tts/xtts-smoke/xtts-cuda-smoke.json
```

### Gateway TTS

`SettingsAwareAudioGenerationGateway` ahora solo entrega `cuda:N` a Voz IA avanzada si:

1. la política permite GPU;
2. GPU para voz está activada;
3. el smoke CUDA local confirma `gpuUsableForXtts()`.

Si no se cumplen esas tres condiciones, Voz IA avanzada usa CPU.

### Configuración

La página `Rendimiento y dispositivo` agrega:

- estado `CUDA Voz IA avanzada`;
- botón `Probar GPU para Voz IA avanzada`;
- mensaje claro cuando Windows detecta GPU pero Python local no tiene CUDA.

### Script auxiliar

Se agrega:

```bat
scripts\37-smoke-cuda-xtts.bat
```

Sirve como verificación manual rápida desde consola. La validación de producto principal sigue viviendo en la app mediante `RunXttsCudaSmokeUseCase`.

## Archivos nuevos

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/XttsCudaSmokeReport.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectXttsCudaSmokeUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/RunXttsCudaSmokeUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/XttsCudaRuntimeProbeGateway.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/XttsCudaRuntimeProbeResult.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/ProcessXttsCudaRuntimeProbeGateway.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/compute/RunXttsCudaSmokeUseCaseTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/application/compute/AssessComputeAccelerationCudaSmokeTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/MotorGpuSmoke1SourceTest.java`
- `scripts/37-smoke-cuda-xtts.bat`

## Criterio de salida

- Sin smoke CUDA local: Voz IA avanzada no promete GPU y usa CPU.
- Con smoke CUDA local exitoso: Voz IA avanzada puede usar `cuda:0` o el dispositivo NVIDIA confirmado.
- La UI distingue GPU candidata de GPU confirmada.
- El manifiesto `xtts-cuda-smoke.json` queda como evidencia de runtime.
