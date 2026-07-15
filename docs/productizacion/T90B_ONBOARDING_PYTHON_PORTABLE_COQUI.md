# T90B — Onboarding Python local para Coqui/XTTS

## Objetivo

Preparar Coqui/XTTS sin pedir al usuario comandos avanzados ni instalaciones globales de Python.

DocuPodcast sigue siendo una aplicación Java. Python queda limitado a un wrapper aislado para sintetizar WAV con Coqui/XTTS.

## Alcance

T90B agrega un onboarding local:

```text
scripts\20-preparar-python-portable-coqui.bat
scripts\21-probar-coqui-xtts.bat
scripts\22-verificar-coqui-xtts-local.bat
tools\xtts-wrapper\requirements-xtts.txt
tools\xtts-wrapper\check_xtts_runtime.py
tools\python\README.md
```

El script `20-preparar-python-portable-coqui.bat` descarga un Python NuGet repo-local dentro de `tools\python`, crea `tools\xtts-wrapper\.venv` e instala dependencias desde `requirements-xtts.txt`.

## Qué no hace

- No instala Python globalmente.
- No modifica PATH.
- No copia modelos pesados dentro de proyectos `.docupodcast`.
- No descarga automáticamente modelos XTTS.
- No reintroduce Whisper/STT.

## Estructura esperada

```text
DocuPodcast Studio/
  tools/
    python/
      python-nuget-3.10.11/
        tools/python.exe
    xtts-wrapper/
      .venv/
      synthesize_xtts.py
      check_xtts_runtime.py
      requirements-xtts.txt
  models/
    tts/
      xtts/
      xtts/speakers/voz-por-defecto.wav
```

## Comandos

```bat
scripts\20-preparar-python-portable-coqui.bat
scripts\22-verificar-coqui-xtts-local.bat
scripts\21-probar-coqui-xtts.bat
```

El reporte de preparación queda en:

```text
target/docupodcast-engine-setup/T90B_COQUI_PYTHON_SETUP_REPORT.md
```

## Criterio de producto

Autocontenido significa que la app usa carpetas propias y guías/scripts propios. No significa que el proyecto del usuario guarde modelos o runtimes.

Los artefactos generados por el usuario siguen dentro del proyecto `.docupodcast`; herramientas, runtimes y modelos viven en instalación/configuración local.
