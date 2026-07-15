# Tanda 47B — Build verde y documento unificado

Esta tanda corrige guardarraíles fuente que habían quedado desalineados después de la modularización GUI y deja explícito el criterio de producto: todo contenido importable se trata primero como documento de trabajo.

## Criterio de producto

DocuPodcast no debe obligar al usuario a decidir desde el inicio si un archivo es “guion” o “documento técnico”. El archivo importado se renderiza como documento y luego el proyecto agrega capas de narración, voz, audio, emoción, imagen o storyboard según el uso real.

## Corrección de tests

- `DocumentComfortReadingSourceTest` ahora valida el botón principal dinámico mediante `PrimaryActionStrip` y `DocuPodcastShellViewModel`.
- `GuidedEngineSettingsSourceTest` y `ModelInstallAssistantSourceTest` validan los estilos a través de `AppStyles`, que es la nueva fuente transversal de clases CSS.

No se cambia comportamiento productivo, audio, exportación, persistencia ni UI funcional.
