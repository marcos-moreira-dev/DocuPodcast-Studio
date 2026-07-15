# Tanda 102 — StatusBar + ReadingZoomControl

## Resumen

Implementa el control inferior derecho de tamano de lectura. La StatusBar ahora combina estado no bloqueante con un `ReadingZoomControl` compacto para aumentar, reducir, restaurar o ajustar con slider el tamano de texto del documento.

## Cambios

- Nuevo `ReadingZoomControl` en `presentation.status`.
- `StatusBarView` recibe el estado de lectura y monta el control a la derecha con `HBox.setHgrow`.
- `DocuPodcastShellView` pasa handlers del ViewModel al StatusBar.
- `DocuPodcastShellViewModel` agrega propiedad `readingFontSize`, constantes 14/18/28 y persistencia en `OperationalSettings`.
- `DocumentWorkspaceView` aplica clases CSS `document-reader-size-*` al workspace.
- `document-page.css` define tamanos de lectura 14 a 28.
- `statusbar.css` estiliza slider, porcentaje y botones.
- `GuiComponentCatalog` registra `StatusBarView` y `ReadingZoomControl`.

## Validacion en entorno ChatGPT

- Revision fuente de StatusBar, ReadingZoomControl, Shell y ViewModel.
- Compilacion focal de fuentes modificadas con stubs JavaFX donde fue necesario.
- Ejecucion reflexiva de tests fuente focales.
- ZIP integro.

No se ejecuto Maven completo porque `mvn` no esta disponible en este entorno. Se recomienda ejecutar `scripts\99-diagnostico-completo.bat` localmente.

## Siguiente tanda

T103 — Inicio propagandistico moderno.
