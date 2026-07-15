# TP2 — Preflight integrado a arranque/configuración

## Objetivo

Convertir el preflight de motores en una señal humana de producto, no solo en un log técnico o en el estado de un script.

La app debe diferenciar:

- **Listo**: motores y media utilizables.
- **Requiere preparación**: faltan modelos, ejecutables o muestras, pero el usuario puede corregirlo siguiendo una acción clara.
- **Error**: no se pudo inspeccionar o falta una base de configuración esencial.

## Cambios

- Se agrega `HumanEnginePreflightState`.
- Se agrega `HumanEnginePreflightSummary`.
- Se agrega `BuildHumanEnginePreflightSummaryUseCase`.
- `SettingsApplicationServices` expone el preflight de arranque y el resumen humano.
- `SettingsDialog` muestra `Estado humano`, `Resumen` y `Siguiente acción` en la sección de motores.

## Alcance visible

Configuración puede mostrar detalles de Coqui/XTTS, Piper y FFmpeg. Documento no debe convertirse en una cabina de diagnóstico.

## Reglas de continuidad

- Coqui/XTTS sigue siendo objetivo de calidad alta.
- Piper sigue siendo fallback liviano.
- FFmpeg prepara media/video.
- Whisper/STT no vuelve al producto visible.
- El preflight puede ejecutarse bien aunque el motor requiera preparación; esos estados no deben confundirse.

## Validación

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado: compile, tests y smoke verde. Si motores reales faltan, debe existir mensaje humano de preparación, no una falla opaca.
