# T115 — Selector CPU/GPU real para Coqui/XTTS

## Objetivo

DocuPodcast ahora detecta CPU/GPU para que el usuario elija dónde procesar Coqui/XTTS. Esta información no es solo decorativa: se usa para construir el argumento `-Device` enviado al wrapper local de XTTS.

## Cambios

- Se agregó el puerto `ComputeDeviceDiscoveryGateway`.
- Se agregó `EnvironmentComputeDeviceDiscoveryGateway` como fallback seguro.
- Se agregó `WindowsComputeDeviceDiscoveryGateway` para consultar `Win32_VideoController` en Windows.
- Se agregó `XttsComputeDeviceMapper` para traducir selección de UI a argumento de Coqui/XTTS.
- Configuración reemplaza el campo manual de dispositivo por un `ComboBox` con `auto`, `cpu` y GPUs detectadas.
- `LocalTtsProcessConfiguration` conserva `{computeDevice}` como ID crudo y usa `{device}` como argumento efectivo para XTTS.
- `ApplicationServicesFactory` usa `WindowsComputeDeviceDiscoveryGateway`.

## Política

- CPU siempre está disponible.
- NVIDIA se mapea a `cuda:n`.
- AMD/Intel se detectan, pero para Coqui/XTTS se usa fallback CPU salvo soporte real posterior.
- Si la GPU no es compatible, la app debe usar CPU honestamente.

## Validación

- Tests de mapper.
- Tests de parseo de GPU Windows.
- Source test de configuración y selección.
