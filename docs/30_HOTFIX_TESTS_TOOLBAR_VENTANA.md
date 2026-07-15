# Hotfix — tests, ventana decorada y toolbar desplazable

Este hotfix corrige tres hallazgos de validación local:

1. Los tests fallaban porque `ImportVoiceSampleUseCaseTest` usaba la fábrica `DocuPodcastProject.empty(String)`, que no existía en el agregado. Se agregó como alias explícito de `createNew(String)` para scaffolding y pruebas.
2. La ventana debe pedir explícitamente `StageStyle.DECORATED` para conservar barra nativa de minimizar, maximizar y cerrar.
3. Las toolbars deben comportarse como las de la referencia DMS: filas desplazables horizontalmente, sin cortar textos de botones y sin jerga interna innecesaria.

Se aplaza para una tanda de polish visual mayor:

- toolbar contextual por workspace con contributors reales;
- mejor espaciado de pantalla de inicio;
- statusbar más rica;
- revisión completa de textos de producto;
- iconografía final.
