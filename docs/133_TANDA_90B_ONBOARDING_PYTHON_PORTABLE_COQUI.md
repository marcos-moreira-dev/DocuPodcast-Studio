# Tanda 90B — Onboarding Python local para Coqui/XTTS

Tanda basada en T90.

## Cambios

- Agrega `scripts/20-preparar-python-portable-coqui.bat`.
- Agrega `scripts/21-probar-coqui-xtts.bat`.
- Agrega `scripts/22-verificar-coqui-xtts-local.bat`.
- Agrega `scripts/tts/setup-xtts-portable-python.ps1`.
- Agrega `tools/xtts-wrapper/requirements-xtts.txt`.
- Agrega `tools/xtts-wrapper/check_xtts_runtime.py`.
- Documenta `tools/python/` como runtime local gestionado por scripts.
- Mejora `xtts-file-to-wav.ps1` para explicar cómo preparar Python cuando falta `.venv`.

## Validación esperada

Suite normal:

```bat
scripts\02-ejecutar-tests.bat
```

Preparar Coqui/XTTS:

```bat
scripts\20-preparar-python-portable-coqui.bat
```

Probar Coqui/XTTS:

```bat
scripts\21-probar-coqui-xtts.bat
```

Smoke real de motores:

```bat
scripts\19-smoke-motores-reales.bat
```

## Nota

T90B no convierte a Python en núcleo de la app. Java sigue siendo el producto; Python solo es runtime aislado para Coqui/XTTS.
