# Python local — DocuPodcast Studio

Este directorio queda reservado para el runtime Python administrado por DocuPodcast.

La preparación se hace con:

```bat
scripts\20-preparar-python-portable-coqui.bat
```

La aplicación y los scripts no deben usar Python global, `py`, `python3` ni el `PATH` del sistema para Coqui/XTTS.

El runtime descargado/extraído queda bajo `tools\python`, y el entorno operativo de XTTS queda en:

```text
tools\xtts-wrapper\.venv\Scripts\python.exe
```
