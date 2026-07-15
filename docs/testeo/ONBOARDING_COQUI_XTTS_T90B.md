# Onboarding Coqui/XTTS — T90B

## Orden recomendado

1. Ejecutar tests normales:

```bat
scripts\02-ejecutar-tests.bat
```

2. Preparar Python local y dependencias Coqui:

```bat
scripts\20-preparar-python-portable-coqui.bat
```

3. Verificar runtime:

```bat
scripts\22-verificar-coqui-xtts-local.bat
```

4. Probar síntesis corta:

```bat
scripts\21-probar-coqui-xtts.bat
```

5. Ejecutar smoke real cuando también existan Piper y FFmpeg:

```bat
scripts\19-smoke-motores-reales.bat
```

## Evidencia

- `target/docupodcast-engine-setup/T90B_COQUI_PYTHON_SETUP_REPORT.md`
- `target/docupodcast-engine-setup/output/coqui-xtts-onboarding.wav`
- `target/docupodcast-engine-setup/logs/`
