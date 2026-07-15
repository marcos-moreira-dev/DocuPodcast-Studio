# Estrategia — Tanda 9

## Decisión

El gateway TTS real debe tratar cada segmento como unidad recuperable y diagnosticable.

## Contrato de diagnóstico

Cada intento de proceso local escribe:

- jobId;
- segmentId;
- intento;
- motor;
- comando resumido;
- exit code;
- duración;
- timeout;
- archivo de salida;
- bytes generados;
- mensaje;
- cola de stdout/stderr.

Archivo:

```text
jobs/JOB-*/logs/process-diagnostics.jsonl
```

## Por qué antes de Voice Library

La biblioteca de voces multiplicará configuraciones. Antes de eso, el gateway debe explicar fallos por proceso, timeout, WAV ausente, WAV vacío o comando mal formado.
