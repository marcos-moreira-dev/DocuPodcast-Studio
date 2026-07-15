# Runtime TTS local

Esta carpeta documenta el contrato del motor TTS real de DocuPodcast Studio.

La aplicación JavaFX no abre una API HTTP ni requiere un backend visible. Cuando se configura un motor real, `LocalTtsProcessAudioGenerationGateway` invoca un proceso local por segmento con un comando configurable.

## Configuración mínima

Puedes configurar el comando por propiedad JVM o variable de entorno:

```bat
set DOCUPODCAST_TTS_COMMAND=runtime\tts\tts_worker.exe --text-file {textFile} --output-file {outputFile} --language {language} --voice {voice}
set DOCUPODCAST_TTS_DISPLAY_NAME=XTTS local empaquetado
set DOCUPODCAST_TTS_LANGUAGE=es
set DOCUPODCAST_TTS_VOICE=VOC-NARRATOR
```

O con Maven/Java:

```bat
mvn javafx:run -Ddocupodcast.tts.command="runtime\tts\tts_worker.exe --text-file {textFile} --output-file {outputFile} --language {language} --voice {voice}"
```

## Placeholders soportados

- `{textFile}` o `{input}`: archivo TXT UTF-8 con el texto del segmento.
- `{outputFile}` o `{output}`: WAV que debe generar el motor.
- `{segmentId}`: ID del segmento, por ejemplo `SEG-001`.
- `{language}`: idioma de la solicitud, por defecto `es`.
- `{voice}` o `{voiceProfileId}`: perfil de voz lógico, por defecto `VOC-NARRATOR`.

## Contrato del worker

El proceso debe:

1. Leer el texto desde `{textFile}`.
2. Escribir un WAV válido en `{outputFile}`.
3. Terminar con exit code 0 si todo salió bien.
4. Terminar con exit code distinto de 0 si falló.
5. No abrir servidores ni requerir interacción de consola.

## Estado actual

La Tanda 8 no incluye modelos ni binarios pesados. Deja el gateway real listo para XTTS/Piper/Python empaquetado o cualquier worker local compatible.

## Diagnóstico y reintentos desde Tanda 9

La Tanda 9 agrega robustez para motores reales:

- `DOCUPODCAST_TTS_MAX_RETRIES` / `-Ddocupodcast.tts.maxRetries`: cantidad de reintentos adicionales por segmento. Por defecto: `1`.
- `DOCUPODCAST_TTS_TIMEOUT_SECONDS` / `-Ddocupodcast.tts.timeoutSeconds`: timeout máximo por ejecución de segmento. Por defecto: `180`.
- Cada intento genera una línea JSONL en `jobs/JOB-*/logs/process-diagnostics.jsonl`.
- El diagnóstico guarda segmento, intento, exit code, duración, timeout, tamaño del WAV, mensaje y cola de stdout/stderr.
- Los WAV ya completados se conservan durante la reanudación.

Ejemplo:

```bat
set DOCUPODCAST_TTS_MAX_RETRIES=2
set DOCUPODCAST_TTS_TIMEOUT_SECONDS=300
```

El worker sigue siendo un proceso local. No se abre API HTTP ni backend visible.
