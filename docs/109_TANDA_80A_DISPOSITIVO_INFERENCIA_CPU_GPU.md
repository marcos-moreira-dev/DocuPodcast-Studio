# Tanda 80A — Dispositivo de inferencia CPU/GPU y rendimiento

## Propósito

T80A introduce un contrato de cerebro para decidir dónde ejecutar cargas locales de inferencia y render: automático, solo CPU, preferir GPU o dispositivo específico. La pantalla Documento sigue siendo un lector narrado simple; CPU/GPU vive en Configuración/Diagnóstico como bodega técnica.

## Cambios principales

- Nuevo paquete `application.compute` con:
  - `ComputeDevicePolicy` (`AUTO`, `CPU_ONLY`, `PREFER_GPU`, `SPECIFIC_DEVICE`).
  - `ComputeDeviceDescriptor` y `ComputeDeviceType`.
  - `ComputePurpose`.
  - `VideoEncoderPolicy` (`AUTO`, `CPU_X264`, `NVIDIA_NVENC`, `INTEL_QSV`, `AMD_AMF`).
  - `ComputeEnvironmentReport`.
  - `InspectComputeEnvironmentUseCase`.
- `OperationalSettings` agrega `ComputeSettings` persistente.
- `PropertiesOperationalSettingsRepository` guarda/lee:
  - `compute.policy`.
  - `compute.selectedDeviceId`.
  - `compute.allowGpuForTts`.
  - `compute.allowGpuForStt`.
  - `compute.allowGpuForVideo`.
  - `video.encoderPolicy`.
- TTS externo agrega placeholders para wrappers:
  - `{computePolicy}`.
  - `{computeDevice}` / `{device}`.
  - `{gpuIndex}`.
- STT/Whisper registra política de cómputo y valida dispositivo específico vacío.
- Video simple registra política/encoder en `RENDER_MANIFEST.json`, `VIDEO_SIMPLE_PLAN.md`, `FFMPEG_RENDER_CONTRACT.md` y `RENDER_STATE.md`.
- `SettingsDialog` agrega una sección de bodega técnica: **Rendimiento / dispositivo**.

## Decisión de producto

El modo recomendado es `AUTO`. El usuario normal no debe escribir CUDA, NVENC, QSV, AMF ni flags crudos en la pantalla Documento. Esos detalles quedan en Configuración.

## Límite honesto

El diagnóstico ligero no ejecuta benchmarks ni `ffmpeg -encoders`. Detecta CPU siempre y GPU por señales de entorno conocidas. Por tanto, el soporte real de un encoder por hardware depende del binario FFmpeg instalado.

## Validación focal

- `InspectComputeEnvironmentUseCaseTest`.
- `OperationalSettingsUseCaseTest`.
- `PropertiesOperationalSettingsRepositoryTest`.
- `LocalTtsProcessConfigurationTest`.
- `WhisperCppConfigurationTest`.
- `BuildVideoRenderCommandPlanUseCaseTest`.
- `ComputeDeviceBrainContractSourceTest`.
