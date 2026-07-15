# Tanda 27 — TTS local robusto

## Objetivo

Robustecer el gateway TTS local por proceso antes de avanzar hacia motores pesados. Esta tanda no integra XTTS/Coqui/Whisper; cierra el contrato base de proceso externo.

## Cambios principales

- Preflight del comando TTS con `LocalTtsProcessPreflight` y `LocalTtsPreflightReport`.
- Validación de placeholders mínimos `{textFile}`/`{input}` y `{outputFile}`/`{output}`.
- Mensaje de diagnóstico en `AudioEngineDescriptor` según resultado de preflight.
- Sanitización de texto por segmento con `TtsTextPreprocessor` antes de escribir `segments/*.txt`.
- Log `tts_text_sanitized` cuando el texto enviado al motor cambia respecto al texto original.
- Seguimiento de procesos activos con `runningProcesses`.
- Cancelación fuerte: `cancel(jobId)` intenta detener el proceso externo activo, no solo esperar al siguiente segmento.
- Diagnóstico `tts_preflight` y `tts_process_destroyed_by_cancel` en los logs del job.

## Alcance

La tanda mantiene el modelo actual de salida:

```text
jobs/<job>/segments/*.txt
jobs/<job>/audio/*.wav
jobs/<job>/audio-manifest.json
jobs/<job>/logs/generation-log.jsonl
```

La concatenación WAV final queda para Tanda 34.

## Tests

- `TtsTextPreprocessorTest`
- `LocalTtsProcessPreflightTest`
- `LocalTtsProcessAudioGenerationGatewaySourceTest` reforzado
- `LocalTtsProcessGatewayDiagnosticsSourceTest` reforzado

## Validación local recomendada

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
