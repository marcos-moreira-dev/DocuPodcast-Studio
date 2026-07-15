# RUNTIME-ARCH-RC1 — Consolidacion de runtime antes de RC

## Objetivo

Dejar por escrito y protegido el contrato final de runtime antes de la puerta RC. Esta tanda no agrega una pantalla nueva ni una promesa visual; consolida los componentes que ya existen para que motores, scripts, diagnostico y empaquetado usen las mismas reglas.

## Componentes transversales obligatorios

- `RuntimeArtifactPaths`: unica autoridad de rutas para `tools`, `models`, `runtime`, `voice-library`, smoke XTTS y scripts TTS.
- `ExternalProcessRunner`: unica puerta nueva para ejecutar procesos locales de Python, FFmpeg, Piper, PowerShell o probes.
- `ExternalProcessFailedException`: error tipado para procesos que devuelven codigo distinto de cero o no producen artefacto valido.
- `ModelArtifactContract`: contrato comun de archivos requeridos para Voz IA avanzada, Voz local simple y Video local.
- `ManagedDownloadService`: contrato comun para descargas reanudables y validables.
- `OperationalSettingsMigrationPolicy`: reparacion de configuracion persistente vieja antes de ejecutar comandos.

## Reglas obligatorias

1. Ninguna ruta critica nueva debe hardcodear `tools/xtts-wrapper`, `models/tts/xtts`, `runtime/tts/xtts-smoke`, `tools/piper`, `tools/ffmpeg`, `voice-library/samples` o `voice-library/tmp-recordings` fuera de las politicas de runtime.
2. Ningun proceso externo nuevo debe ejecutarse con `new ProcessBuilder(...)` fuera de infraestructura de procesos, salvo legacy documentado mientras se migra.
3. La app no debe volver a construir ni pasar `model.pth/model.pth`.
4. Las settings antiguas que apunten a scripts localizados o rutas terminadas en `model.pth` deben repararse antes de probar o generar audio.
5. La UI normal no debe mostrar nombres tecnicos de motores. Diagnostico avanzado y documentacion tecnica si pueden hacerlo.
6. Los scripts PowerShell deben permanecer ASCII-safe para Windows PowerShell 5.1.

## Criterio de cierre RC

Antes de RC, diagnostico completo debe confirmar:

- Maven compile OK.
- Maven tests OK.
- Smoke automatico cerebro OK.
- Preflight arranque motores OK.
- Piper y FFmpeg locales OK.
- Sin warnings CSS por tokens faltantes conocidos.
- Sin comandos visibles sin handler.
- Sin motores mostrados como usables si no pasan readiness real.
- Sin rutas legacy absolutas en comandos auditados.

## Alcance pendiente despues de esta consolidacion

Esta tanda no obliga a migrar todos los procesos legacy en una sola pasada. Las migraciones profundas quedan en tandas especificas:

- `RUNTIME-ARCH-RC1B` si se decide migrar mas `ProcessBuilder` legacy al runner.
- `PACKAGING-MEMORY-RC1` para app portable y memoria.
- `RC-GATE1` para la puerta final.
