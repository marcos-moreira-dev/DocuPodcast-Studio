# TP1 — RuntimePathResolver / layout de instalación

TP1 introduce una capa pequeña y centralizada para resolver rutas de runtime sin depender solamente de `Path.of(".")`.

## Objetivo

Preparar DocuPodcast Studio para tres modos de ejecución:

1. repositorio de desarrollo;
2. carpeta portable;
3. instalador/app-image.

## Cambios

- Nuevo paquete `application.runtime`.
- `RuntimePathResolver` resuelve la raíz de la aplicación por prioridad:
  1. system property `docupodcast.app.root`;
  2. variable de entorno `DOCUPODCAST_APP_ROOT`;
  3. code source;
  4. directorio de trabajo como fallback.
- `ApplicationRuntimeLayout` define rutas estándar:
  - `tools/`;
  - `models/`;
  - `scripts/`;
  - `examples/`;
  - `tools/ffmpeg/bin/ffmpeg.exe`;
  - `tools/piper/piper.exe`;
  - `tools/xtts-wrapper/...`.
- `InfrastructureServicesFactory` usa esta raíz para TTS y FFmpeg.
- `SettingsDialog` usa la misma raíz para preflight de motores.
- Se corrige `GuiComponentSurface.WELCOME`, faltante tras T113.

## Alcance

TP1 no empaqueta todavía motores/modelos ni instala FFmpeg/Piper/Coqui. Solo fija el contrato de rutas para que las próximas tandas de productización no dependan de rutas accidentales del repositorio.

## Próximo paso

TP2 debe llevar el preflight de arranque/configuración a mensajes más humanos y accionables.
