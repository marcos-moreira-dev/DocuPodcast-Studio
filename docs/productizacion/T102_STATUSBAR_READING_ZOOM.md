# T102 — StatusBar + ReadingZoomControl implementado

## Objetivo

T102 implementa el control visual de tamano de lectura en la StatusBar inferior, a la derecha, sin convertirlo en zoom de canvas ni en redimensionamiento global de la aplicacion.

## Alcance implementado

- `StatusBarView` deja de ser solo mensaje de estado y aloja un `ReadingZoomControl` compacto.
- Nuevo componente `presentation.status.ReadingZoomControl` con botones `-`, `100%`, `+`, slider y porcentaje.
- `DocuPodcastShellViewModel` expone `readingFontSizeProperty()` y acciones de aumento, reduccion, reset y set directo.
- El tamano de lectura se carga desde `OperationalSettings.readingDocument().baseFontSize()`.
- Al cambiar el tamano desde la StatusBar, se guarda de vuelta en `operational-settings.properties` mediante `SaveOperationalSettingsUseCase`.
- `DocumentWorkspaceView` aplica clases CSS `document-reader-size-14` a `document-reader-size-28` para refluir texto de lectura sin estilos inline.
- `GuiComponentCatalog` registra `StatusBarView` y `ReadingZoomControl` como componentes de `STATUS_BAR`.

## Decisiones de producto

- El control cambia tamano de lectura/fuente; no escala el documento como lienzo ni cambia imagenes, railes o toda la aplicacion.
- El rango visible queda alineado con `OperationalSettings`: 14 px a 28 px, con 18 px como 100%.
- La StatusBar sigue siendo una superficie secundaria: estado no bloqueante + confort de lectura, no barra de comandos principales.

## Fuera de alcance

- No implementa zoom de pagina, ancho de lectura ni layout final del Documento.
- No implementa playbar flotante final.
- No convierte el Ribbon ni MenuBar en superficies de zoom.
- No modifica el contrato de motores ni procesos largos.

## Archivos principales

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/ReadingZoomControl.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java
src/main/resources/css/statusbar.css
src/main/resources/css/document/document-page.css
```

## Tests y guardarrailes

- `StatusBarReadingZoomT102SourceTest`
- `GuiComponentCatalogTest`

Los guardarrailes verifican que el shell monte el control, que el ViewModel persista la preferencia, que Documento use clases CSS para refluir texto y que no se usen estilos inline para el tamano dinamico.

## Validacion recomendada

```bat
scripts\99-diagnostico-completo.bat
```

## Siguiente tanda recomendada

T103 — Inicio propagandistico moderno.
