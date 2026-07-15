# Roadmap post 59A-B — Componentes transversales y cerebro de la app

## Contexto

Tanda 59 fijó principios rectores. Tanda 59A inició la migración de acciones repetibles hacia componentes GUI transversales. La validación local detectó una regresión de source test, corregida en 59A-B.

A partir de aquí, el rediseño no debe confundirse con decorar pantallas. Hay que separar el trabajo en dos planos.

## Plano 1: catálogo transversal de front-end

El catálogo de componentes no es simplemente “botones bonitos”. Debe definir un lenguaje de interfaz reutilizable:

- acciones primarias/secundarias/destructivas;
- barras de acciones;
- controles de transporte;
- filas de rail;
- badges informativos y métricos;
- tarjetas de diagnóstico;
- estados vacíos;
- headers de sección;
- paneles de ayuda/diagnóstico;
- reglas de uso, no solo clases JavaFX.

La aplicación visual completa debe esperar hasta que el producto confirme mejor su cara final.

## Plano 2: cerebro de la app

En una aplicación JavaFX local no hay backend web, pero sí existe un equivalente funcional:

```text
domain
application/use cases
coordinators
services
repositories
jobs
audio/playback/video pipelines
settings/configuration
capability policies
validation policies
```

Ese cerebro debe ordenarse antes de un rediseño visual profundo. El foco prioritario es reducir `DocuPodcastShellViewModel`, separar coordinadores y asegurar que el flujo documento fuente → Documento narrable → proyección interna/audio → playback → persistencia → exportación no dependa de una sola clase gigantesca.

## Orden recomendado

1. T59A-B — hotfix build verde.
2. T59B — catálogo/contrato transversal completo: componentes, acciones, criterios y límites.
3. T60 — auditoría del cerebro: mapa de dominio, application, coordinadores, jobs, audio, playback, video, persistencia y configuración.
4. T61 — refactor prioritario del cerebro sin cambiar comportamiento visible.
5. T62 — auditoría funcional del flujo real con smoke usuario.
6. T63 — rediseño UI aplicado con componentes transversales.
7. T64 — round-trip avanzado, packaging y RC.

## Regla guía

La cara puede evolucionar; el cerebro debe ordenarse ya.
