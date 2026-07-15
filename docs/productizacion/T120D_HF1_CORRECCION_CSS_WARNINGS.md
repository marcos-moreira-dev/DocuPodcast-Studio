# T120D-HF1 — Corrección CSS warnings JavaFX

## Objetivo

Eliminar advertencias de JavaFX CSS observadas al ejecutar la aplicación después de T120C, antes de iniciar el rediseño fuerte de la vista Voces.

## Contexto

La consola mostraba advertencias como:

- no se podía resolver `-dp-text`;
- no se podía resolver `-dp-primary`;
- no se podía resolver `-dp-muted`;
- `examples-dialog` tenía problemas al convertir `-fx-background-color`;
- `example-project-card` tenía problemas al convertir `-fx-border-color` y hover;
- `settings-engine-status-badge` tenía problemas con `-fx-background-color`.

Estas advertencias no rompían Maven ni el smoke automático, pero ensuciaban la consola y eran una señal de deuda visual. Para una app que se acerca a despliegue, la consola no debe llenarse con errores de CSS al abrir ventanas comunes como ejemplos o configuración.

## Cambio aplicado

Se agregaron aliases CSS en `tokens.css` para cubrir los tokens usados por módulos auxiliares/heredados:

```css
-dp-surface
-dp-border
-dp-primary
-dp-primary-soft
-dp-text
-dp-muted
-dp-muted-text
-docu-chip-background
```

La corrección es deliberadamente conservadora: no rediseña ejemplos, configuración ni Voces. Solo estabiliza tokens y evita warnings JavaFX conocidos.

## Archivos tocados

- `src/main/resources/css/tokens.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/CssWarningsT120DHf1SourceTest.java`
- `README.md`
- `AI_HANDOFF.md`
- `VALIDATION.md`
- `docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md`

## Tests agregados

- `CssWarningsT120DHf1SourceTest`

Este test protege que los aliases usados por ejemplos/configuración estén definidos y no vuelvan a quedar huérfanos.

## Criterio de aceptación

- Maven/tests deben seguir verdes.
- Abrir la app no debe repetir los warnings CSS conocidos de `examples-dialog`, `example-project-card`, `settings-engine-status-badge`, `-dp-text`, `-dp-primary` y `-dp-muted`.
- No se introduce rediseño visual en esta hotfix.
