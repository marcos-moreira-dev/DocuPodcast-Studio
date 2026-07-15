# RF2 — Coordinadores de presentación / selección documental

Estado: implementada sobre RF1 verde.

## Objetivo

Reducir responsabilidades del `DocuPodcastShellViewModel` moviendo reglas de presentación de selección documental a un coordinador dedicado, sin cambiar la UX principal de Documento ni los componentes visuales transversales.

## Cambios principales

- Se agrega `DocumentSelectionCoordinator` en `presentation.shell.workflow`.
- El coordinador concentra:
  - normalización de bloque seleccionado;
  - preview seguro de texto seleccionado;
  - etiquetas de selección de oración, fragmento y capa;
  - etiqueta de ubicación fuente;
  - copy de la acción primaria del documento.
- `DocuPodcastShellViewModel` conserva propiedades JavaFX y handlers, pero delega reglas de texto/estado de selección.
- Se corrige el guardarraíl de T102 para reconocer que la persistencia del tamaño de lectura vive en `ReadingComfortCoordinator` desde RF1.
- `DocuPodcastShellViewModel` baja a menos de 2450 líneas.

## Contrato preservado

- Documento sigue siendo la superficie principal.
- Las capas se guardan en el proyecto, no en el Word original.
- No se reintroducen nombres técnicos de motores en la interfaz gráfica normal.
- No se toca CSS ni componentes visuales.

## Tests

- `PresentationCoordinatorsRf2SourceTest`
- `StatusBarReadingZoomT102SourceTest` actualizado para RF1/RF2

## Validación focal realizada

- `javac --release 21` de dominio/aplicación/infraestructura/coordinadores no-FX.
- Compilación focal de tests RF2/T102 con stubs JUnit.
- Ejecución reflexiva de 8 métodos de tests fuente seleccionados.
