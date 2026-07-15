# T118 — Piper modo intermedio compatible

## Objetivo

Aterrizar Piper como motor local intermedio/liviano. Piper no reemplaza a Coqui/XTTS como motor principal de lectura de calidad alta y no debe prometer capacidades de clonación por muestra humana, emociones o estilos expresivos.

## Cambios principales

- `PiperSetupReadinessReport` describe el estado local de Piper.
- `InspectPiperSetupReadinessUseCase` verifica `piper.exe`, wrapper `piper-file-to-wav.ps1`, voz `.onnx` y metadatos `.onnx.json`.
- `SelectPiperAsEngineUseCase` selecciona `engineMode=piper` con nombre visible `Piper — lectura intermedia/liviana`.
- `SettingsApplicationServices` expone los nuevos casos de uso.
- `SettingsDialog` agrega bloque `Asistente Piper` con botones reales `Verificar Piper` y `Usar Piper intermedio`.
- Catálogos de configuración y modelo actualizan el lenguaje de Piper a intermedio/liviano.

## Regla UX

Si Piper está activo, Documento, Sidebar, Voces y Configuración no deben mostrar opciones propias de Coqui/XTTS como emociones expresivas, clonación por muestra humana o voces personalizadas basadas en `speaker_wav`. Piper usa modelos de voz `.onnx` + `.onnx.json`.

## Criterio de aceptación

Piper puede verificarse y seleccionarse desde Configuración como modo intermedio/liviano. La UI no promete capacidades falsas y el contrato local no depende de PATH global.
