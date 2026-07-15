# Tanda 37 — Seguimiento de oración activa tipo MuseScore

## Propósito

Hacer que la pantalla Documento acompañe la lectura narrada: cuando el playback avanza a un nuevo segmento, el bloque fuente del documento se resalta y se mantiene en una franja cómoda, un poco arriba del centro visual, para que el usuario no tenga que perseguir el texto.

## Cambios

- Se agrega `ActiveReadingAnchor`, una política de presentación que calcula la posición de scroll para mantener el bloque activo en una zona estable de lectura.
- `DocumentWorkspaceView` escucha `playbackCursorProperty()` y usa el `sourceBlockIds` del `NarrationSegment` activo para encontrar el bloque original del Word.
- El bloque activo recibe la clase visual `document-block-active-reading`.
- El texto del bloque activo se subraya mientras la reproducción apunta a ese segmento.
- El scroll se aplica con `Platform.runLater(...)` para acompañar el layout JavaFX sin saltos prematuros.
- Se agrega una nota discreta en el documento: el seguimiento activo mantiene el segmento narrado en una zona estable de lectura.

## Criterio de producto

La app debe sentirse como un lector narrado: el usuario abre un Word, pulsa escuchar y la vista acompaña la oración o segmento activo sin obligarlo a navegar por módulos técnicos.

## Límites

Esta tanda todavía trabaja a nivel de bloque/segmento narrable. La selección exacta por oración, el prebuffer real por chunks y las asignaciones parciales de voz/audio/imagen quedan para tandas posteriores.
