# Contrato CPU/GPU — T80A

## Principio rector

DocuPodcast puede usar motores locales pesados, pero la experiencia principal no debe convertirse en una cabina técnica. La política de dispositivo pertenece a la bodega de Configuración/Diagnóstico.

## Políticas

- `AUTO`: valor recomendado; deja que cada motor use su fallback seguro.
- `CPU_ONLY`: fuerza intención de no usar GPU.
- `PREFER_GPU`: permite que motores compatibles intenten GPU y caigan a CPU si no está disponible.
- `SPECIFIC_DEVICE`: permite seleccionar un identificador, por ejemplo `gpu-0`, si el usuario avanzado sabe qué hace.

## Propósitos

- TTS.
- STT.
- VIDEO_RENDER.
- GENERAL_DIAGNOSTIC.

## Video

El video simple mantiene 2K por defecto y agrega política de encoder:

- `AUTO`.
- `CPU_X264`.
- `NVIDIA_NVENC`.
- `INTEL_QSV`.
- `AMD_AMF`.

El manifest debe declarar `renderDevicePolicy`, `requestedEncoder`, `effectiveEncoder`, `hardwareAccelerationRequested`, `hardwareAccelerationReady` y `fallbackReason`.

## TTS/STT

TTS por proceso puede recibir `{computePolicy}`, `{computeDevice}` y `{gpuIndex}` para wrappers locales. STT registra política y dispositivo, pero no inventa flags incompatibles entre motores.

## Regla de honestidad

Si el sistema no puede verificar soporte de GPU, debe decirlo como advertencia o fallback; no debe prometer aceleración.
