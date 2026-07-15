# MOTOR-PERF1 / MOTOR-GPU1 — CPU/GPU honesto

## Objetivo

Evitar que la app prometa aceleración GPU solo porque detectó una tarjeta gráfica. La detección de hardware ahora se interpreta como candidatura, no como confirmación de uso real.

## Cambios

- Se agregó `AssessComputeAccelerationUseCase` para separar:
  - GPU detectada.
  - GPU candidata.
  - GPU confirmada por prueba real.
- Se agregó `ComputeAccelerationAssessment` como reporte productivo para Configuración.
- La página `Rendimiento / dispositivo` ahora muestra:
  - estado honesto de Voz IA avanzada;
  - estado honesto de Video;
  - etiqueta de promesa GPU;
  - advertencias consolidadas.
- GPU NVIDIA puede quedar como candidata de voz, pero requiere prueba real antes de prometer aceleración.
- GPU AMD/Intel no se declara compatible para Voz IA avanzada; se cae a CPU hasta tener soporte probado.
- Video con encoder acelerado queda como candidato hasta verificación del componente local.

## Regla de producto

> Detectar GPU no equivale a usar GPU. La app solo debe prometer GPU cuando exista una prueba real del motor o del componente de video.

## Fuera de alcance

- No ejecuta benchmark real.
- No valida todavía uso efectivo de GPU por el motor Python.
- No revalida descarga de Voz IA avanzada.
- No cambia exportación MP4 ni audio comprimido.

## Validación esperada

- `AssessComputeAccelerationUseCaseTest`.
- `MotorPerf1HonestGpuSourceTest`.
- Diagnóstico completo verde.
