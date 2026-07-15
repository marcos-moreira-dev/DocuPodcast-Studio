# Tanda 67 — Configuración operativa real

## Objetivo

Convertir la configuración en una pieza real del cerebro desktop, no solo en una pantalla informativa. La pantalla Documento sigue siendo un lector narrado simple; la configuración funciona como bodega técnica persistente para lectura, buffer, TTS, STT, FFmpeg, almacenamiento y diagnóstico.

## Decisiones

- Los documentos fuente siguen en modo solo lectura.
- La configuración no edita Word/PDF/Markdown/TXT.
- Los ajustes operativos se guardan fuera de los documentos fuente, en `operational-settings.properties`.
- El bootstrap lee estos ajustes al iniciar y los usa para cablear TTS, STT y preflight.
- La UI de configuración muestra valores persistidos y reporte de validación, pero no invade el lector.

## Implementación

Se agregan:

- `OperationalSettings`.
- `OperationalSettingsRepository`.
- `LoadOperationalSettingsUseCase`.
- `SaveOperationalSettingsUseCase`.
- `ValidateOperationalSettingsUseCase`.
- `OperationalSettingsValidationReport`.
- `SettingsApplicationServices`.
- `PropertiesOperationalSettingsRepository`.

`InfrastructureServicesFactory` carga la configuración operativa al iniciar y la usa para crear:

- `LocalTtsProcessConfiguration`.
- `WhisperCppConfiguration`.

## Criterio de salida

La configuración ya tiene contrato persistente y se puede validar sin convertir la pantalla principal en cabina técnica.
