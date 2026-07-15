# T105 - Playbar flotante, Ribbon respirable y Documento expandido

## Objetivo

Cerrar la correccion posterior a T104 antes de seguir con Sidebar/Rail: la superficie superior no debe truncar textos, Inicio no debe mostrar jerga de implementacion y el Documento debe ocupar casi toda el area central disponible como hoja de lectura tipo Word.

## Cambios

- `RibbonView`, `RibbonButton`, `RibbonGroup` y CSS del Ribbon crecen horizontal y verticalmente para evitar puntos suspensivos y reservar espacio para iconografia futura.
- `WelcomeWorkspaceView` elimina badges centrales `Local`, `Fuente intacta`, `V1: lector + capas` y el consejo inferior asociado, porque se percibian como jerga de implementacion.
- `DocumentWorkspaceView` monta `FloatingReadingControlBar` en un `StackPane` sobre el lector (`document-reading-stage`) en lugar de incrustarla como top rigido de `BorderPane`.
- El documento se expande: `DOCUMENT_PAGE_MAX_WIDTH = 1280.0`, margenes laterales del host reducidos y padding interno mas sobrio.
- Se restaura el indicador compacto de buffer (`streamingBufferStatusProperty` + `document-listen-buffer-status`) para no romper el contrato de streaming playback.

## No hace

- No implementa Sidebar final.
- No implementa Rail redimensionable.
- No implementa playback por oracion exacta.
- No cambia el cerebro de audio/playback.

## Validacion

- Tests fuente focales con javac y runner reflexivo.
- Guardarrailes para que no vuelvan badges de implementacion en Inicio.
- Guardarrailes para hoja expandida y control flotante.
