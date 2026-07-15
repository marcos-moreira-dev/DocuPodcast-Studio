# T114-HF2 — Retiro total Whisper/STT

## Base

T114-HF1 — Build verde + no-guion visible mínimo.

## Motivo

La decisión de producto vigente es explícita: DocuPodcast Studio no usa Whisper, STT, Speech-to-Text ni audio a texto. La aplicación abre documentos Word/PDF/Markdown/TXT, los prepara para lectura y genera voz con motores TTS. Mantener Whisper como “infraestructura histórica” genera ruido arquitectónico, configuración engañosa, tests que protegen una capacidad fuera de alcance y riesgo de que reaparezca en UX.

## Decisión de producto

- Whisper/STT no pertenece al producto DocuPodcast.
- No queda como roadmap visible.
- No queda como infraestructura futura.
- No queda en Configuración.
- No queda en preflight.
- No queda en modelo de settings.
- No queda en scripts.
- La grabación se conserva solo para voz humana, muestras TTS o notas/referencias de audio.

## Cambios técnicos

### Bootstrap e infraestructura

Se retira el cableado de Speech-to-Text de:

- `ApplicationServices`
- `ApplicationServicesFactory`
- `InfrastructureServices`
- `InfrastructureServicesFactory`
- `module-info.java`

Se eliminan del build principal los paquetes:

- `application/stt`
- `infrastructure/stt`

También se elimina el script:

- `scripts/stt/whisper-file-to-text.ps1`

### Configuración operativa

`OperationalSettings` deja de tener `SttEngineSettings`.

`ComputeSettings` deja de tener `allowGpuForStt`.

`PropertiesOperationalSettingsRepository` deja de leer/escribir:

- `stt.whisperExecutable`
- `stt.whisperModel`
- `stt.language`
- `stt.timeoutSeconds`
- `compute.allowGpuForStt`

La configuración queda centrada en:

- lectura;
- buffer;
- TTS;
- video/FFmpeg;
- CPU/GPU para TTS y video;
- almacenamiento;
- diagnóstico.

### Modelos y asistentes

`ModelFolderContract.recommended()` queda limitado a:

- `xttsHighQuality()`
- `piperLightweight()`

Se retira `whisperLocal()`.

### Grabación y normalización

`RecordingPurpose` deja de incluir `SPEECH_TO_TEXT_SOURCE`.

`AudioRecordingReference` deja de exponer `canFeedSpeechToText()`.

`RecordingActionPlan` deja de exponer `speechToTextCandidate()`.

`AudioNormalizationProfile` conserva solo `ASSIGNABLE_AUDIO`.

La normalización FFmpeg queda orientada a audio asignable, playback, media y diagnóstico; no a transcripción.

### Tests

Se actualizan tests que protegían Whisper como infraestructura histórica. La nueva regla es inversa: el producto principal no debe conservar Whisper/STT.

Se agrega `NoWhisperSttT114Hf2SourceTest`, que valida:

- `src/main/java` no contiene `Whisper`;
- `src/main/java` no contiene `SpeechToText`;
- `src/main/java` no contiene `SPEECH_TO_TEXT`;
- `src/main/java` no contiene `allowGpuForStt`;
- no existen paquetes `application/stt` ni `infrastructure/stt`;
- no existe `scripts/stt`;
- `ModelFolderContract` recomienda solo motores de voz/media.

## Cambio asociado del diagnóstico local T114-HF1

El diagnóstico `20260602-224724` fallaba por un único source test:

- `DocumentLayerAssignmentWorkflowSourceTest`

El test aún esperaba el texto antiguo:

- `No se encontró un segmento narrable`

Se actualiza para esperar el texto alineado con producto:

- `No se encontró un fragmento preparado`

## Validación realizada en entorno de generación

No hay Maven disponible en este entorno, pero se ejecutó validación focal:

- compilación `javac --release 21` de paquetes `application`, `domain`, `infrastructure` y `bootstrap` no JavaFX;
- compilación focal de tests `application`, `domain`, `infrastructure` con stubs JUnit;
- compilación focal de tests `productization` con stubs JUnit;
- búsqueda de `Whisper`, `SpeechToText`, `STT`, `stt`, `SPEECH_TO_TEXT` y `allowGpuForStt` en `src/main/java` y `scripts`: sin resultados.

## Validación requerida en Windows

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado:

- Maven compile OK.
- Maven tests OK.
- Smoke automático cerebro OK.
- Preflight arranque motores OK.
- Piper y FFmpeg locales OK o reporte humano de preparación.

## Reglas para siguientes tandas

- No reintroducir Whisper/STT.
- No reintroducir Audio a texto.
- No reintroducir `stt.*` en settings.
- No reintroducir `allowGpuForStt`.
- No reintroducir scripts `scripts/stt`.
- La configuración debe centrarse en Coqui/XTTS, Piper, FFmpeg y CPU/GPU para TTS/video.
