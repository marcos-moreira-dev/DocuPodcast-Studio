# XTTS wrapper local — DocuPodcast Studio

Este directorio contiene solo el wrapper mínimo para invocar Coqui/XTTS desde Java.

## Regla de runtime

DocuPodcast no usa Python global para Coqui/XTTS. El único runtime permitido es el entorno local creado por:

```bat
scripts\20-preparar-python-portable-coqui.bat
```

Ruta esperada:

```text
tools\xtts-wrapper\.venv\Scripts\python.exe
```

## Archivos

- `synthesize_xtts.py`: wrapper pequeño que recibe texto, voz de referencia y genera WAV.
- `check_xtts_runtime.py`: verificación del runtime local.
- `requirements-xtts.txt`: dependencias del entorno local.
- `.venv/`: entorno local generado; no debe versionarse manualmente.

## Uso

No ejecutar `python synthesize_xtts.py` desde consola global. El script puente `scripts\tts\xtts-file-to-wav.ps1` valida que el Python usado sea el local del repositorio.
