# Tanda 55C — Hotfix configuración de voz y build verde

## Objetivo

Corregir el guardarraíl `VoiceCalibrationSettingsSourceTest` que esperaba que la pantalla de Configuración mencionara explícitamente el motor potente `XTTS / Coqui`, el respaldo `Piper` y el flujo de grabación de muestras autorizadas.

## Cambios

- `SettingsDialog` ahora muestra explícitamente:
  - `Motor potente — XTTS / Coqui`
  - `Motor semipotente — Piper`
  - `Muestras de voz — Haz clic para grabar tu voz o una voz autorizada: neutral, enojada, triste, feliz e intrigada`
- La calibración de voz sigue viviendo en Configuración, no en el Documento principal.
- No se modifica audio, TTS real, reproducción, video, persistencia ni asignaciones.

## Criterio de producto protegido

La pantalla principal sigue siendo para leer/escuchar documentos. La configuración conserva la “bodega técnica”: motores, modelos, voces, velocidad, pausas y diagnóstico.
