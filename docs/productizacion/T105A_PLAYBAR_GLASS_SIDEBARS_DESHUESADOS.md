# T105A — Playbar glass y sidebars deshuesados

## Objetivo

Corregir los fallos reportados por el diagnostico local `20260602-015457` y aplicar la realimentacion visual antes de avanzar a la siguiente superficie.

La tanda no cambia el cerebro del sistema. Ajusta el frente Documento para que la playbar no invada la hoja y para que los dos laterales dejen de parecer cabina tecnica.

## Cambios

- `FloatingReadingControlBar` queda top-pinned en el escenario de lectura y no se estira verticalmente sobre la pagina.
- La barra usa un tratamiento glass/translucido: blanco al 52%, borde suave y sombra difusa. JavaFX no ofrece backdrop blur real por CSS, asi que se simula con transparencia y difuminado visual.
- `DocumentWorkspaceView` reduce el margen superior de la playbar y la mantiene alineada arriba.
- El inspector izquierdo se deshuesa: titulos mas cortos, texto menos tecnico, boton de ocultar compacto y rail lateral mas legible.
- El rail derecho se deshuesa: titulo `Visual`, subtitulo corto y secciones compactas de navegación visual; desde T109 queda sin `Asignadas`, con `Storyboard` e `Imagenes`.
- Se corrigen source tests desalineados con T105 y se restaura continuidad documental exigida por guardarrailes historicos.

## Archivos principales

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java
src/main/resources/css/components/actions.css
src/main/resources/css/document/document-page.css
src/main/resources/css/document-reader.css
src/main/resources/css/components/media-rail.css
```

## Validacion esperada

La tanda debe dejar verde:

```bat
scripts\99-diagnostico-completo.bat
```

Si queda verde, la siguiente tanda recomendada es T106 — Sidebar izquierdo contextual final, ya sobre una base menos ruidosa.
