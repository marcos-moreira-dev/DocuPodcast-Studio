# Preflight de arranque de motores — T90E

Ejecutar:

```bat
scripts\23-preflight-arranque-motores.bat
```

Reporte:

```text
target\docupodcast-engine-startup-preflight\T90E_STARTUP_ENGINE_PREFLIGHT_REPORT.md
```

El script no usa Python global. Si falta el runtime local de Coqui/XTTS, debe indicar que se ejecute:

```bat
scripts\20-preparar-python-portable-coqui.bat
```
