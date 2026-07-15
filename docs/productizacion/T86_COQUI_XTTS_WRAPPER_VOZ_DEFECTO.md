# T86 - Coqui XTTS wrapper real y voz por defecto

## Propósito de producto

DocuPodcast necesita una voz de calidad alta para que el esfuerzo del producto tenga sentido. Piper permite una demo rápida, pero **Coqui XTTS** queda como ruta obligatoria para narración más realista.

## Contrato de carpetas

```text
tools/xtts-wrapper/synthesize_xtts.py        wrapper Python mínimo
scripts/tts/xtts-file-to-wav.ps1             puente PowerShell usado por Java
models/tts/xtts/                             modelo local XTTS
models/tts/xtts/speakers/voz-por-defecto.wav muestra WAV runtime para fallback XTTS
samples/voices/advanced-presets/             presets oficiales de Voz IA avanzada
samples/theatre/maps/mapa-espacial.png       mapa espacial base para futuras obras teatrales
```

## Voz por defecto

La fuente antigua `samples/voices/default/source/voz-por-defecto.mp4` fue retirada. La voz runtime `models/tts/xtts/speakers/voz-por-defecto.wav` se obtiene ahora desde el preset oficial:

```text
source: samples/voices/advanced-presets/hombre_adulto_personaje_narrativo/neutral.wav
runtime: models/tts/xtts/speakers/voz-por-defecto.wav
```

La app debe usar `voz-por-defecto.wav` solo como fallback administrado cuando el comando XTTS no reciba una muestra explícita. Para voces avanzadas prediseñadas, la biblioteca usa los presets oficiales y sus muestras por tono.

## Cómo se activa

Configurar:

```properties
tts.engineMode=xtts
```

o:

```properties
tts.engineMode=coqui
```

Si no hay `tts.commandTemplate`, `InfrastructureServicesFactory` deriva el comando con `XttsTtsCommandTemplate`.

## Dependencias externas pendientes

Para que funcione fuera de mock se necesita:

1. Python local obligatorio en `tools/xtts-wrapper/.venv/`; no se usa Python global.
2. Paquete Coqui TTS instalado en ese Python.
3. Modelo XTTS en `models/tts/xtts/`.
4. Muestra de voz WAV en `models/tts/xtts/speakers/`.
5. Configuración de CPU/GPU validada por preflight.

## No descarga automática

Esta tanda **no descarga modelos automáticamente**. El objetivo es dejar el contrato y el wrapper listos. La descarga/instalación guiada debe tratarse en una tanda posterior con catálogo, hashes y advertencias de licencia.

## Siguiente

Los presets oficiales de Voz IA avanzada quedan versionados en `samples/voices/advanced-presets/`. La capa teatral futura podrá vincular una voz real a un alias dramático, por ejemplo una voz llamada `Mario Alonzo` usada en una obra como `El villano`.
