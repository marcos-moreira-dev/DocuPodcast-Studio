# Tanda 14 implementada — Guía integrada + recursos IA

Esta tanda agrega la ayuda integrada tipo escritorio y la exportación de recursos IA oficiales.

## Guía integrada

Se agregaron paquetes `application.guide`, `infrastructure.guide` y `presentation.guide`.

La guía carga temas Markdown desde `src/main/resources/help/topics/` y expone un diálogo con dos pestañas:

- Contenido.
- Buscar.

El orden de temas es intencional: Word/DOCX aparece antes que Markdown/IA porque las notas del usuario viven principalmente en Word.

Temas oficiales:

- Primeros pasos.
- Importar notas desde Word.
- Perfil de lectura.
- Guion narrable.
- Voces, personajes y estilos.
- Voces autorizadas.
- Storyboard vivo.
- Generación de audio.
- Reproducción sincronizada.
- Exportaciones.
- Recursos IA y Markdown.
- Solución de problemas.
- Glosario.

## Recursos IA

Se agregó `application.resources` e `infrastructure.resources` con catálogo y exportador de recursos oficiales.

La acción `Ayuda > Exportar recursos IA...` copia recursos desde classpath a una carpeta elegida y genera `00_indice_recursos_ia.md`.

Recursos incluidos:

- Gramática `docupodcast-script-v1`.
- Plantilla de guion no importable porque contiene placeholders.
- Prompt maestro.
- Ejemplo académico importable.
- Ejemplo teatral/storyboard importable.
- Referencia de voces autorizadas.

## Regla de producto

Markdown sigue siendo puente humano/IA. El proyecto editable completo sigue viviendo en `.docupodcast.json` más assets/jobs/manifests.

## Validación

Se agregaron tests para catálogo de guía, búsqueda local, exportación de recursos, ausencia de placeholders en recursos importables y visibilidad de acciones UI.
