# Tanda 9 implementada

La Tanda 9 robustece el TTS real por proceso local con reintentos y diagnóstico persistente.

Nuevo archivo por job:

```text
jobs/JOB-*/logs/process-diagnostics.jsonl
```

Nuevas variables:

```text
DOCUPODCAST_TTS_MAX_RETRIES
DOCUPODCAST_TTS_TIMEOUT_SECONDS
```

La UI de Audio muestra diagnósticos del proceso para depurar workers XTTS/Piper/Python sin abrir servidores ni APIs.
