# PLAYBACK-HF6 — Reuso de WAVs y manifest activo

Esta tanda corrige el flujo donde el audio se exporta correctamente pero la reproducción interna no encuentra fragmentos listos.

## Cambios

- El manifest de playback por unidades omite TTS del mismo segmento cuando una unidad usa audio externo, preservando el contrato histórico de clips de usuario.
- `Reproducir fragmento` reconstruye el manifest desde el job persistido antes de declarar que un fragmento no tiene audio.
- La acción principal del documento vuelve a consultar el job más reciente si el manifest activo todavía no contiene la selección actual.
- La navegación anterior/siguiente sigue usando el manifest activo y no debe regenerar audio si ya existe WAV.

## Criterio de producto

Si un WAV ya fue generado o exportable, el usuario debe poder reproducirlo desde Documento sin volver a calcularlo innecesariamente.
