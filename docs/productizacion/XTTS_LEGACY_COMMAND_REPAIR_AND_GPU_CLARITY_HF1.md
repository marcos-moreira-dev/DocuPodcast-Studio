# XTTS-LEGACY-COMMAND-REPAIR-HF1 + PIPER-GPU-CLARITY-HF1

## Contexto

Se observó en app real que la prueba de Voz IA avanzada seguía fallando con:

```text
FileNotFoundError: .../recursos locales IA avanzada/model.pth/model.pth
```

El problema no era todavía CUDA/GPU. El flujo fallaba antes de cargar el modelo. La ruta llegaba al wrapper Python como si `model.pth` fuera una carpeta y luego Coqui/XTTS intentaba abrir `model.pth/model.pth`.

También se observó confusión entre motores:

- Voz local simple/Piper sí descarga y genera audio, pero trabaja en CPU.
- Seleccionar GPU no acelera Piper.
- Voz IA avanzada/XTTS puede usar GPU NVIDIA solo si el Python local tiene PyTorch CUDA y el smoke CUDA pasa.
- La GPU Intel puede mostrar actividad por composición gráfica/video/UI, pero no es CUDA para XTTS.

## Decisión de producto

DocuPodcast debe diferenciar claramente:

```text
GPU detectada por Windows
GPU seleccionada por el usuario
GPU compatible con el motor
GPU confirmada por smoke real
GPU usada en la generación
```

Para Piper:

```text
Voz local simple/Piper recibe el dispositivo solicitado; la aceleración real depende del binario local.
```

Para XTTS:

```text
Voz IA avanzada usa una ruta segura en Automático; en Dispositivo específico intenta el dispositivo solicitado y el runtime informa si no lo soporta.
```

## Corrección aplicada

### 1. Reparación de comandos legacy/localizados

Algunos settings antiguos conservan comandos como:

```text
scripts/tts/Voz IA avanzada-file-to-wav.ps1
componentes locales IA avanzada-wrapper
recursos locales IA avanzada/model.pth
```

Aunque el flujo moderno usa:

```text
scripts/tts/xtts-file-to-wav.ps1
tools/xtts-wrapper
models/tts/xtts
```

Ahora `XttsTtsCommandTemplate` detecta esos comandos legacy/localizados y reconstruye el comando administrado desde el `applicationRoot` actual, evitando rutas viejas de otra carpeta ZIP.

Detecciones cubiertas:

```text
xtts-file-to-wav
synthesize_xtts
xtts-wrapper
Voz IA avanzada-file-to-wav
componentes locales IA avanzada-wrapper
recursos locales IA avanzada
```

### 2. Script de compatibilidad

Se agregó:

```text
scripts/tts/Voz IA avanzada-file-to-wav.ps1
```

Este script existe solo como puente de compatibilidad. Redirige al script administrado:

```text
scripts/tts/xtts-file-to-wav.ps1
```

Además normaliza `ModelDir` si apunta a `model.pth`, usando la carpeta padre.

### 3. Defensa Java final

`LocalTtsProcessConfiguration` conserva una defensa final para normalizar argumentos:

```text
-ModelDir .../model.pth
--model-dir .../model.pth
-ModelDir=.../model.pth
--model-dir=.../model.pth
```

antes de ejecutar PowerShell/Python.

### 4. Prueba de voz usa applicationRoot para CUDA

`SettingsAwareVoiceTestSynthesisGateway` ahora consulta la política CPU/GPU usando el `applicationRoot` real. Esto evita que el smoke CUDA se inspeccione desde una ruta equivocada.

### 5. Mensaje de Piper/dispositivo

El catálogo humano de motores ahora comunica que Voz local simple/Piper recibe el dispositivo solicitado y que la aceleración real depende del binario local.

## Criterio de aceptación

- Un comando legacy con `Voz IA avanzada-file-to-wav.ps1` debe reconstruirse como comando administrado moderno.
- El comando final no debe contener `model.pth/model.pth`.
- El comando final no debe conservar rutas de una carpeta vieja.
- El script legacy debe existir como compatibilidad.
- Piper debe comunicar que trabaja en CPU.
- XTTS debe seguir requiriendo smoke CUDA aprobado antes de prometer GPU.

## Tests asociados

```text
XttsTtsCommandTemplateTest.localizedLegacyAdvancedVoiceCommandIsRebuiltAsManagedPortableCommand
XttsLegacyCommandRepairSourceTest
PiperCpuOnlyClaritySourceTest
LocalTtsProcessConfigurationModelDirTest
```
