# T90E — Preflight de arranque de motores

## Objetivo

T90E agrega un contrato inicial para que la aplicación pueda verificar al arrancar si los motores locales están disponibles antes de exponer flujos de voz/media.

## Piezas agregadas

```text
AppStartupEnginePreflightUseCase
StartupEnginePreflightReport
StartupEnginePreflightItem
scripts\23-preflight-arranque-motores.bat
scripts\tts\preflight-startup-engines.ps1
```

## Qué verifica

- Python local Coqui/XTTS.
- Wrapper `synthesize_xtts.py`.
- Script puente `xtts-file-to-wav.ps1`.
- Carpeta de modelo Coqui/XTTS.
- Voz por defecto.
- Piper local como fallback opcional.
- FFmpeg local como soporte media/video.

## Política

El preflight de arranque no descarga sin permiso ni instala cosas globales. Debe devolver mensajes humanos para guiar el setup:

```text
Falta preparar motor de voz.
Ejecuta scripts\20-preparar-python-portable-coqui.bat.
```

## Alcance

T90E no rediseña la GUI. Crea el contrato para una futura pantalla/asistente de preparación al iniciar la aplicación.
