# Tanda 43 — Configuración guiada de motores TTS/STT/Whisper

## Objetivo

Separar la complejidad técnica de motores de voz y transcripción de la pantalla principal del documento narrado. La experiencia operativa sigue siendo abrir, leer y escuchar el Word; la configuración de motores vive en la ventana de Configuración como bodega técnica guiada.

## Cambios

- Se agrega catálogo guiado de motores en `presentation.settings`:
  - `EngineSetupOption`
  - `EngineSetupCatalog`
  - `EngineSetupCard`
- `SettingsDialog` agrega el módulo **Motores guiados**.
- El catálogo recomienda:
  - **Calidad alta — XTTS / Coqui** como motor potente prioritario para voz más humana y personajes.
  - **Liviano — Piper** como respaldo local semipotente.
  - **STT — Whisper local** para audio a texto guiado.
- La UI describe acciones esperadas del asistente:
  - descargar modelo,
  - importar manualmente,
  - abrir carpeta de modelos,
  - probar motor,
  - verificar/preflight.
- No se hardcodean URLs externas. La disponibilidad de modelos se tratará con catálogo versionado, importación manual y checksums en una tanda posterior.
- Se agregan estilos modulares en `css/components/settings-shell.css`.

## Alcance no incluido

Esta tanda no descarga modelos reales, no instala XTTS/Piper/Whisper y no ejecuta preflight real desde UI. Define la superficie guiada para evitar que el usuario normal tenga que escribir líneas de comando.

## Criterio de producto

La pantalla principal sigue limpia. Los motores, modelos pesados, carpetas, checksums y diagnósticos pertenecen a Configuración o asistentes.
