# PF6B — Smoke real desde GUI asistido

Estado: implementada como primer corte seguro sobre PF5C.

## Objetivo

Crear una validación de smoke que se complete desde la interfaz real sin simular clics falsos ni declarar éxito cuando todavía falta motor.

## Cambios

- `BuildGuiSmokeChecklistUseCase` inspecciona Voz IA avanzada, Voz local simple y FFmpeg.
- Genera `dist/release-candidate/PF6B_GUI_SMOKE_CHECKLIST.md`.
- Configuración → Motores de voz agrega `Smoke real desde GUI` con el botón `Generar checklist smoke GUI`.
- El checklist marca pasos `READY`, `MANUAL` o `BLOCKED`, para evitar promesas falsas.
- `scripts/36-smoke-gui-asistido.bat` queda como apoyo técnico y el release candidate lo invoca para dejar una guía mínima; la validación principal ocurre dentro de la app.

## Criterio

PF6B no es automatización falsa de JavaFX. Es un smoke asistido y honesto: el usuario debe generar prueba de voz real, abrir documento corto, escuchar y confirmar artefactos.
