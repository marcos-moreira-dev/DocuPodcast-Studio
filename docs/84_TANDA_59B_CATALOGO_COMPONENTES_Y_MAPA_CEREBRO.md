# Tanda 59B — Catálogo transversal y mapa del cerebro

## Tipo de tanda

Documental/arquitectura protegida. No aplica un rediseño visual masivo y no cambia comportamiento productivo.

## Motivo

Después de validar T59A-B en verde, se ajusta el foco estratégico: DocuPodcast necesita dos frentes separados.

1. Un catálogo/contrato de componentes transversales, entendido como lenguaje de interfaz y no solo como botones personalizados.
2. Un mapa del cerebro de la app, equivalente al backend en una aplicación JavaFX desktop.

## Archivos agregados

```text
docs/productizacion/CATALOGO_COMPONENTES_TRANSVERSALES.md
docs/productizacion/MAPA_CEREBRO_APP.md
docs/productizacion/AUDITORIA_CEREBRO_PRIORITARIA.md
docs/productizacion/ROADMAP_POST_T59B_CEREBRO_COMPONENTES.md
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/TransversalComponentCatalogSourceTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/AppBrainMapSourceTest.java
```

## Decisiones

- El catálogo transversal no se reduce a crear botones bonitos.
- La UI puede seguir evolucionando, pero el cerebro debe ser estable y auditable.
- El refactor prioritario debe enfocarse en dominio, casos de uso, coordinadores, persistencia, audio, playback, video, jobs y configuración.
- El rediseño visual aplicado queda después de definir el cerebro y sus reglas.

## Próximo paso recomendado

T60 — Auditoría ejecutable del cerebro.
