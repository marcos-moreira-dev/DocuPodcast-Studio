# PLAYBACK-SPEED4 / DOC-WORD-STYLE1

## Objetivo

Corregir dos problemas operativos detectados por el usuario:

1. `Reproducir desde selección` o el playbar podían quedarse en el primer fragmento o repetir un fragmento tras cambios de velocidad.
2. Los títulos/subtítulos importados desde Word no se diferenciaban lo suficiente del texto corrido dentro del lector.

## Cambios

### Playback

- `playFromSelectedSegment()` ahora prioriza la selección real del documento (`selectedDocumentSegmentOrSelected`) antes de caer al segmento general.
- Se agrega una guardia corta de transición (`playbackTransitionGuardUntilNanos`) para evitar falsos finales mientras el reproductor reinicia el mismo WAV al cambiar velocidad.
- `tickPlayback()` usa una transición explícita a la siguiente cue con `transitionToCue(...)`.
- Al avanzar de cue, se detiene el reproductor actual, se limpia el reloj y recién se inicia el siguiente WAV.
- `playCueForCursor(...)` arma una ventana corta de transición tras cada `play(...)` exitoso.

### Documento Word

- `document-page.css` resalta `TITLE`, `HEADING` y `SUBHEADING` con tipografía en negrita y tonos marrones sobrios.
- Se mantiene el aspecto de “documento limpio” sin volver la página una maqueta recargada.

## Validación sugerida

1. Generar chunks para un documento largo.
2. Usar `Reproducir desde selección`.
3. Confirmar que avanza por varios fragmentos consecutivos.
4. Cambiar entre `1x`, `1.5x` y `1.75x` durante la reproducción.
5. Verificar que no repite el mismo fragmento y que continúa con el siguiente.
6. Abrir un DOCX con títulos/subtítulos y confirmar que se ven más resaltados que la prosa.
