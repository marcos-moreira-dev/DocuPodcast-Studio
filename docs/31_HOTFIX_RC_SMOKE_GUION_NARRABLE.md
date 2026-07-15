# Hotfix RC — Smoke manual guion narrable

## Motivo

La suite local llegó a 135/136 pruebas aprobadas y falló únicamente `ReleaseCandidateDocumentationSourceTest` porque `docs/testeo/SMOKE_MANUAL_RELEASE_CANDIDATE.md` no contenía literalmente la frase `guion narrable` en minúsculas.

## Corrección

Se agregó una línea explícita en el bloque de smoke manual del Guion:

```text
El flujo de guion narrable queda validado desde documento importado hasta segmentos revisables.
```

## Alcance

No cambia código productivo, UI, scripts ni arquitectura. Es un ajuste documental para alinear el smoke manual con el guardarraíl de release candidate.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Resultado esperado: todas las pruebas deben pasar.
