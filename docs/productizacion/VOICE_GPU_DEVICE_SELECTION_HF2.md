# VOICE-GPU-DEVICE-HF2 — selector CPU/GPU compartido para motores de voz

## Propósito

Esta corrección ajusta el contrato de rendimiento después de las pruebas de `VOICE-CHUNKS-HF1`.
La regla correcta es que el selector de CPU/GPU pertenece a los **procesos de voz**, no solo a Voz IA avanzada.

- Voz IA avanzada usa GPU NVIDIA automáticamente solo si el Python autocontenido confirma CUDA; en dispositivo específico intenta lo solicitado.
- Voz local simple recibe también el dispositivo seleccionado.
- Si el runtime local de Voz local simple no soporta aceleración GPU, puede ignorar el hint o fallar con diagnóstico propio; la app no promete aceleración falsa.

## Problema detectado

La interfaz había quedado demasiado restrictiva al mostrar que Voz local simple usaba CPU y que el selector de GPU solo aplicaba a motores compatibles. Eso no reflejaba la intención de producto: el usuario debe poder elegir CPU/GPU para voz, y cada motor debe usar el dispositivo si su runtime lo soporta.

El caso observado fue que el sistema parecía usar una GPU secundaria/integrada en lugar de la NVIDIA seleccionada. Por eso la app debe propagar explícitamente la selección de dispositivo al proceso local.

## Cambios técnicos

### `PiperTtsCommandTemplate`

El comando gestionado de Voz local simple ahora incluye placeholders de cómputo:

```text
-ComputePolicy {computePolicy} -Device {computeDevice} -GpuIndex {gpuIndex}
```

### `scripts/tts/piper-file-to-wav.ps1`

El wrapper acepta esos parámetros y exporta hints de entorno:

```text
DOCUPODCAST_COMPUTE_POLICY
DOCUPODCAST_COMPUTE_DEVICE
DOCUPODCAST_PIPER_DEVICE
CUDA_VISIBLE_DEVICES
NVIDIA_VISIBLE_DEVICES
HIP_VISIBLE_DEVICES
ROCR_VISIBLE_DEVICES
ONEAPI_DEVICE_SELECTOR
```

Esto no inventa GPU donde el runtime no la tenga, pero permite que una build capaz de usar NVIDIA, AMD o Intel reciba el índice solicitado.

### UI

La vista de Voces y Configuración ya no dicen que Voz local simple es CPU-only. El mensaje correcto es:

> Voz local simple recibe el dispositivo seleccionado; la aceleración real depende del runtime local.

## Validación esperada

1. En Configuración, el selector debe decir `Dispositivo para voz`.
2. La opción `Permitir GPU para voz` aplica a motores de voz.
3. En Vista Voces, Voz local simple debe mostrar el dispositivo solicitado, no CPU fijo.
4. La prueba CUDA sigue siendo específica para Voz IA avanzada.
5. El wrapper de Voz local simple recibe política/dispositivo/índice de GPU.
