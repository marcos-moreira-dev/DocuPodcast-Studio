# Memoria — Tanda 9

Se implementó diagnóstico operativo para el gateway TTS real. La Tanda 8 dejó el proceso local configurable; la Tanda 9 agrega robustez: reintentos por segmento, diagnóstico JSONL por intento, timeout visible, lectura desde UI y configuración `DOCUPODCAST_TTS_MAX_RETRIES`.

Decisión: antes de construir Voice Library, el motor real debe ser observable y reanudable. El primer fallo de un worker XTTS/Piper no debe dejar al usuario sin explicación.

El mock sigue disponible si no hay comando TTS configurado.
