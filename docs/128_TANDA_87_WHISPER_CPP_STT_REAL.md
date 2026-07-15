# Tanda 87 — whisper.cpp STT real end-to-end

## Objetivo

T87 cierra el camino técnico para que DocuPodcast Studio pueda convertir audio local en texto usando `whisper.cpp`. Esta tanda no agrega otro motor ni cambia la experiencia principal del Documento. El objetivo es dejar operativo el flujo:

```text
audio local → normalización WAV → whisper.cpp → transcript TXT → artefactos dentro del proyecto
```

## Decisión de producto

La ruta STT oficial V1 queda limitada a `whisper.cpp` por simplicidad operativa, ejecución local y compatibilidad con el modelo de app autocontenida. No se agregan motores alternativos en esta tanda.

Coqui/XTTS sigue como motor de voz de calidad alta para TTS. El supuesto de uso documentado para este proyecto es uso local/demostrativo/no venta directa de modelos ni voces como mercancía. Eso no elimina la obligación de revisar licencias si en el futuro se redistribuyen modelos, wrappers o audios de terceros.

## Qué ya existía

Antes de T87 ya existían:

- `SpeechToTextGateway`
- `TranscribeAudioToTextUseCase`
- `WhisperCppConfiguration`
- `WhisperCppSpeechToTextGateway`
- `JavaSoundAudioNormalizer`
- configuración operativa para ejecutable, modelo, idioma y timeout

T87 no reemplaza esa arquitectura. La fortalece, agrega test end-to-end con CLI falsa y deja un wrapper operativo/documental para pruebas manuales.

## Cambios agregados

### Wrapper manual

Se agrega:

```text
scripts/stt/whisper-file-to-text.ps1
```

Este script:

1. Valida el ejecutable `whisper.cpp`.
2. Valida el modelo local.
3. Valida el audio de entrada.
4. Ejecuta `whisper.cpp` con `-otxt`.
5. Verifica que el transcript exista y no esté vacío.

### Gateway robusto para Windows

`WhisperCppSpeechToTextGateway` ahora contempla ejecutables `.cmd`/`.bat` en Windows mediante `cmd.exe /c`. Esto permite testear el contrato de CLI con un fake ejecutable y, en el futuro, envolver `whisper.cpp` con scripts si hace falta.

### Test end-to-end falso pero ejecutable

Se agrega `WhisperCppSpeechToTextGatewayTest`, que no requiere descargar modelos reales. El test crea:

- proyecto temporal;
- audio WAV de prueba;
- modelo local simulado;
- ejecutable falso de whisper que recibe `-of` y genera el `.txt` esperado.

Con eso valida que el gateway:

- cree `stt/input/`;
- cree `stt/transcripts/`;
- cree `stt/logs/`;
- guarde todo dentro de la carpeta del proyecto;
- lea el transcript final;
- devuelva `SpeechToTextResult` útil.

## Rutas operativas esperadas

```text
tools/whisper-cpp/whisper-cli.exe
models/stt/whisper/<modelo>.bin
```

Ejemplo de configuración:

```properties
stt.whisperExecutable=tools/whisper-cpp/whisper-cli.exe
stt.whisperModel=models/stt/whisper/ggml-base.bin
stt.language=es
stt.timeoutSeconds=240
```

## Qué no se hizo

- No se descargó whisper.cpp automáticamente.
- No se incrustó un modelo real dentro del ZIP.
- No se agregó OCR.
- No se cambió la interfaz principal.
- No se convirtió el Documento en panel técnico.

## Validación manual futura

Cuando el entorno tenga `whisper-cli.exe` y un modelo real:

1. Abrir Configuración.
2. Seleccionar ejecutable y modelo.
3. Probar transcripción corta.
4. Verificar que se creen transcript y logs dentro del proyecto.
5. Confirmar que el texto transcrito pueda usarse como documento o material de trabajo.

## Próxima tanda

La siguiente tanda debe ser **T88 — FFmpeg real para media**, porque el flujo usuario incluye `Audio del computador` y `Extraer audio de video…`.
