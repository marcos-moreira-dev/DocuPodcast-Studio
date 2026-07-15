# PF4B — Smoke post-app-image y runtime portable

## Objetivo

Cerrar el primer corte de ejecutable portable con una verificación posterior a `jpackage`: el usuario final debe poder abrir la carpeta portable, hacer doble clic en `run-docupodcast-studio.bat` y que la app resuelva su raíz desde `DOCUPODCAST_APP_ROOT`, no desde PowerShell ni desde el repositorio.

## Cambios

- Nuevo `scripts/34-smoke-app-portable-runtime.bat`.
- Verifica `dist/app-image/DocuPodcastStudio` y `dist/portable/DocuPodcastStudio`.
- Verifica `DocuPodcastStudio.exe`, `run-docupodcast-studio.bat`, `tools`, `models`, `scripts/tts` y `PORTABLE_MANIFEST.txt`.
- Verifica que el launcher declare `DOCUPODCAST_APP_ROOT=%%~dp0`.
- `scripts/16-release-candidate.bat` ejecuta este smoke después del smoke RC instalable.
- Reporte generado: `dist/release-candidate/PF4B_PORTABLE_RUNTIME_SMOKE_REPORT.md`.

## Criterio de producto

PF4B no instala motores ni descarga modelos. Su alcance es confirmar que la app portable queda autocontenida como carpeta ejecutable y que el runtime root se resuelve desde el launcher.
