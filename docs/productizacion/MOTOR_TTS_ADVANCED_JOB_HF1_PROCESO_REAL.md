# MOTOR-TTS-ADV-HF1 — Generación avanzada no queda muda en 0/1

## Contexto

Tras descargar correctamente el modelo de Voz IA avanzada, el usuario pudo iniciar la generación desde Documento, pero el overlay quedaba en `0/1` sin actividad aparente de CPU/GPU y al cerrar la app se reportaba un job en ejecución.

## Cambios

- El mapeo de dispositivo para Voz IA avanzada ya no entrega el valor opaco `auto` al wrapper Python. Mientras no exista una prueba real de aceleración GPU, el valor automático cae a `cpu`.
- El wrapper Python `tools/xtts-wrapper/synthesize_xtts.py` ahora normaliza el dispositivo y emite fases visibles en stdout: inicio, carga de texto, importación del paquete, carga de modelo, device, síntesis y WAV generado.
- El script PowerShell `scripts/tts/xtts-file-to-wav.ps1` fuerza `PYTHONUNBUFFERED` y `PYTHONIOENCODING=utf-8`, y reporta los parámetros principales antes de invocar Python.
- `LocalTtsProcessAudioGenerationGateway` lee la salida del proceso de forma incremental, conserva cola de salida acotada, revisa cancelación cada 500 ms y destruye árbol de procesos en cancelación/timeout.
- El timeout efectivo de Voz IA avanzada sube a mínimo 900 segundos para evitar matar una primera carga real del modelo antes de tiempo.

## Fuera de alcance

- No se cambia la UI de Documento.
- No se cambia playback.
- No se toca la descarga del modelo.
- No se toca el índice visual en esta corrección, porque el foco es desbloquear la generación real.
