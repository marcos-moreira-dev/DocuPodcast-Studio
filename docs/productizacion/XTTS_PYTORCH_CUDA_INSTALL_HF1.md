# XTTS-PYTORCH-CUDA-INSTALL-HF1

## Propósito

Preparar un camino explícito para instalar PyTorch con CUDA dentro del Python local de Voz IA avanzada, sin tocar Python global.

## Diagnóstico de producto

- Voz local simple/Piper recibe el dispositivo solicitado; la aceleración real depende del binario local.
- Voz IA avanzada/XTTS usa GPU NVIDIA automáticamente solo si el Python local tiene PyTorch CUDA y el smoke `torch.cuda.is_available()` queda aprobado. En `Dispositivo específico`, intenta el dispositivo solicitado.
- Una GPU detectada por Windows no implica que XTTS pueda usar CUDA.

## Scripts agregados

- `scripts/tts/setup-xtts-pytorch-cuda.ps1`
- `scripts/39-preparar-pytorch-cuda-xtts.bat`

El script usa `tools/xtts-wrapper/.venv/Scripts/python.exe`, registra log en `runtime/tts/xtts-smoke/xtts-pytorch-cuda-install.log` y no toca Python global.

## Flujo recomendado

1. Preparar Voz IA avanzada y modelo XTTS.
2. Generar WAV de prueba en CPU.
3. Ejecutar `scripts\39-preparar-pytorch-cuda-xtts.bat`.
4. Ejecutar `scripts\37-smoke-cuda-xtts.bat` o el botón `Probar GPU para Voz IA avanzada`.
5. En Automático, solo si el smoke queda aprobado Documento puede usar `cuda:0`; en `Dispositivo específico`, Documento intenta la selección manual.

## Regla UX

Si el usuario pide `Preferir GPU` pero PyTorch local es CPU-only, la app debe avisar y usar CPU. Si el usuario pide `Dispositivo específico`, la app no cambia la intención: ejecuta el runtime con ese dispositivo y deja el error/diagnóstico visible si no lo soporta.
