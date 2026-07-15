# Tanda 59 — Principios rectores de rediseño y scaffolding

## Objetivo

Sentar los criterios que guiarán el rediseño de DocuPodcast antes de ejecutar cambios grandes de UI o refactor. La tanda no intenta cerrar producto ni rediseñar todas las pantallas; fija la brújula para que las siguientes tandas no sigan acumulando scaffolding visible.

## Decisión central

DocuPodcast debe conservar su arquitectura interna, pero presentarse como:

```text
micro Word narrado + capas multimedia opcionales + fábrica local detrás
```

No debe presentarse como una consola de jobs, manifest, chunks, offsets, render packages y gateways.

## Aportes de esta tanda

- Documento rector: `docs/productizacion/PRINCIPIOS_RECTORES_DISENO_SCAFFOLDING.md`.
- Roadmap post T59: `docs/productizacion/ROADMAP_POST_T59_REDISSENO_GUIADO.md`.
- Registro de tanda en documentación raíz y memorias.
- Guardarraíl `DesignScaffoldingPrinciplesSourceTest`.
- Guardarraíl `CssTokenAliasCoverageSourceTest`.
- Higiene CSS mínima: aliases de compatibilidad para tokens detectados como huérfanos durante smoke UI.

## Principios fijados

1. Documento primero.
2. Menos scaffolding visible.
3. Complejidad progresiva.
4. Acciones por intención, no por tecnología.
5. Componentes GUI transversales.
6. SideDock/rail como apoyo, no como pared de opciones.
7. Contrato honesto de capacidades.
8. Persistencia antes de prometer flujo avanzado.
9. Diseño claro, sobrio y de escritorio.
10. Refactor guiado por uso real.

## Hallazgo de smoke incorporado

La base T58C pasó tests localmente, pero al abrir la app aparecieron advertencias CSS por tokens no resueltos o valores incompatibles. Esta tanda agrega aliases de compatibilidad en `tokens.css` para no dejar tokens huérfanos como `-dp-muted-text`, `-docu-accent-dark`, `-docu-bg-subtle`, `-docu-bg-shell` y otros usados por módulos existentes.

## Próxima tanda recomendada

T59A — Componentes GUI transversales.

La regla será: las vistas pueden componer acciones, pero no crear botoneras o estilos repetidos si ya existe o debe existir un componente transversal.
