# Tanda 90 — Smoke real de motores

Base: T89.

Resumen:

- Agrega smoke opt-in para Coqui/XTTS, Piper y FFmpeg reales.
- Genera evidencia en `target/docupodcast-real-engines-smoke/`.
- Mantiene la suite normal sin dependencia de modelos locales.
- No reintroduce audio a texto ni nuevas promesas visibles.
- Documenta rutas por defecto, variables/properties y criterio de aceptación.

Validación esperada:

```bat
scripts\02-ejecutar-tests.bat
scripts\19-smoke-motores-reales.bat
```

El segundo comando requiere motores/modelos locales reales.
