# T104 — Documento limpio tipo lector Word

## Objetivo

T104 consolida el workspace Documento como superficie de lectura de escritorio: una hoja centrada, clara y cómoda, sin cabina técnica visible ni aspecto de landing page.

La tanda no cambia el cerebro ni promete playback por oración perfecto. Se limita a ordenar la experiencia visual del documento para que las siguientes tandas puedan construir playbar flotante, sidebar contextual y rail final sobre una base limpia.

## Cambios principales

- `DocumentWorkspaceView` usa `StackPane pageHost` para centrar la hoja y limitar su ancho visual.
- La página queda más parecida a una hoja de lectura: fondo blanco, borde sobrio, sombra suave y margen superior/inferior más cómodo.
- El encabezado del documento queda mínimo: título, chip `Fuente solo lectura`, chip de refresco y estado de escucha compacto.
- Se elimina el estado de escucha excesivo dentro de la hoja; el buffer detallado queda fuera de la página principal.
- La selección y seguimiento activo usan scroll geométrico basado en bounds (`localToScene`) para acercar el bloque/oración a una posición estable bajo Ribbon/playbar.
- Se mantiene el tooltip técnico, pero no se convierte la hoja en cockpit.
- El Ribbon se corrige tras prueba visual local: queda más respirable que T103A, aproximadamente 20–30 px más alto, con botones más anchos para evitar puntos suspensivos.

## Alcance protegido

T104 no implementa todavía:

- Playbar flotante final.
- Sidebar contextual final.
- Rail derecho redimensionable.
- Playback por `unitId`/oración exacta.
- Refactor grande de `DocuPodcastShellViewModel`.

## Archivos principales

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonGroup.java
src/main/resources/css/document/document-page.css
src/main/resources/css/components/ribbon.css
src/main/resources/css/components/actions.css
```

## Guardarraíles

Se agrega `DocumentCleanT104SourceTest` y se actualizan tests fuente desalineados por T103/T103A:

- `ReaderUiRedesignSourceTest`.
- `DocumentNarratedExperienceSourceTest`.
- `EditableSettingsAndFrontHonestySourceTest`.
- `ModernVisualThemeSourceTest`.
- `RibbonBaseT101SourceTest`.

## Próxima tanda

T105 — Playbar flotante final.
