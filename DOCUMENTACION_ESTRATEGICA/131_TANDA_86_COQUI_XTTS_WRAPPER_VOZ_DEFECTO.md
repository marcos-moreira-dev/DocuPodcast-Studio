# Tanda 86 — Coqui XTTS wrapper y voz por defecto autorizada

## Objetivo

Cerrar el primer contrato operativo de **Coqui XTTS** como motor de voz de calidad alta. Piper queda como respaldo liviano, pero la ruta de producto que debe sonar bien usa Coqui/XTTS mediante un wrapper local.

Esta tanda no descarga modelos pesados ni instala Python por el usuario. La app prepara la estructura, el wrapper y el contrato para que el motor pueda activarse cuando el usuario coloque dependencias y modelos en las carpetas previstas.

## Decisión clave

El archivo recibido `voz por defecto.mp4` es realmente un contenedor MP4 con pista de audio AAC. Se conserva como fuente en:

```text
samples/voices/default/source/voz-por-defecto.mp4
```

Y se normaliza a WAV mono 24 kHz para Coqui/XTTS en:

```text
models/tts/xtts/speakers/voz-por-defecto.wav
```

Ese WAV queda como **muestra de voz propia autorizada por el usuario** y como voz por defecto para el wrapper XTTS cuando el `voiceProfileId` sea `VOC-NARRATOR` o esté vacío.

## Archivos nuevos

```text
src/main/java/.../infrastructure/audio/XttsTtsCommandTemplate.java
scripts/tts/xtts-file-to-wav.ps1
tools/xtts-wrapper/synthesize_xtts.py
models/tts/xtts/speakers/voz-por-defecto.wav
samples/voices/default/source/voz-por-defecto.mp4
src/test/java/.../infrastructure/audio/XttsTtsCommandTemplateTest.java
src/test/java/.../productization/CoquiXttsDefaultVoiceSourceTest.java
```

## Flujo de ejecución previsto

Cuando el usuario configure:

```properties
tts.engineMode=xtts
```

o

```properties
tts.engineMode=coqui
```

DocuPodcast deriva una plantilla de comando si el campo crudo `tts.commandTemplate` está vacío:

```text
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tts/xtts-file-to-wav.ps1 \
  -Python tools/xtts-wrapper/.venv/Scripts/python.exe \
  -Wrapper tools/xtts-wrapper/synthesize_xtts.py \
  -ModelDir models/tts/xtts \
  -SpeakerWav models/tts/xtts/speakers/voz-por-defecto.wav \
  -Text {textFile} \
  -Output {outputFile} \
  -Language {language} \
  -Device {device}
```

Si no existe `.venv/Scripts/python.exe`, el template usa `python` como fallback para que el usuario pueda usar una instalación global o un entorno activado.

## Qué hace el wrapper Python

`tools/xtts-wrapper/synthesize_xtts.py`:

1. Recibe un archivo de texto del segmento.
2. Recibe una muestra de voz WAV autorizada.
3. Recibe el directorio local del modelo XTTS.
4. Importa `TTS.api.TTS` de forma perezosa, solo al sintetizar.
5. Genera un WAV de salida con `tts_to_file`.

La importación perezosa permite que el preflight lea el wrapper aunque el entorno Python todavía no tenga Coqui instalado.

## Qué no hace esta tanda

- No instala Python.
- No instala PyTorch.
- No descarga XTTS.
- No resuelve CUDA automáticamente.
- No clona voces de terceros.
- No empaqueta modelos pesados dentro de `.docupodcast`.

## Criterio ético y legal

La muestra `voz-por-defecto.wav` se trata como voz propia autorizada por el usuario. Si en el futuro se importan voces de terceros, cada muestra debe quedar como asset con consentimiento/notas de uso.

## Relación con T85

T85 habilitó Piper para una primera demo local liviana. T86 habilita la ruta de calidad alta mediante Coqui XTTS. El orden de resolución del comando es intencional:

1. Si `engineMode` es `xtts` o `coqui`, intentar `XttsTtsCommandTemplate`.
2. Si `engineMode` es `piper`, intentar `PiperTtsCommandTemplate`.
3. Si hay comando crudo, respetarlo.
4. Si no hay comando, usar mock.

## Validación

Tests nuevos:

```text
XttsTtsCommandTemplateTest
CoquiXttsDefaultVoiceSourceTest
```

También se corrige la documentación que hacía fallar `AiVoiceTranscriptionRoadmapSourceTest` añadiendo referencias explícitas a `T83` y `Release Candidate real` en el handoff.
